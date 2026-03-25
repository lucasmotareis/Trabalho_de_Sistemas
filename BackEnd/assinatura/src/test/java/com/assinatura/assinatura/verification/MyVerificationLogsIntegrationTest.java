package com.assinatura.assinatura.verification;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class MyVerificationLogsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("DELETE FROM verification_logs");
        jdbcTemplate.update("DELETE FROM signatures");
        jdbcTemplate.update("DELETE FROM user_keys");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldRequireAuthenticationForMyVerificationLogsEndpoint() throws Exception {
        mockMvc.perform(get("/me/verification-logs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void shouldReturnOnlyLogsLinkedToTheLoggedInUser() throws Exception {
        SignedFixture fixtureA = createSignedFixture("only-mine-a");
        SignedFixture fixtureB = createSignedFixture("only-mine-b");

        String emailA = uniqueEmail("verifier-a");
        String emailB = uniqueEmail("verifier-b");
        String password = "Senha@123";
        signup(emailA, password);
        signup(emailB, password);
        Cookie sessionA = loginAndGetSessionCookie(emailA, password);
        Cookie sessionB = loginAndGetSessionCookie(emailB, password);

        mockMvc.perform(get("/verify/{publicId}", fixtureA.publicId()).cookie(sessionA))
                .andExpect(status().isOk());
        mockMvc.perform(post("/verify")
                        .cookie(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                fixtureB.publicId(),
                                fixtureB.originalText(),
                                fixtureB.signatureBase64())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/verify/{publicId}", fixtureA.publicId()).cookie(sessionB))
                .andExpect(status().isOk());
        mockMvc.perform(get("/verify/{publicId}", fixtureB.publicId()))
                .andExpect(status().isOk());

        Long userAId = userIdByEmail(emailA);
        int userALogs = countVerificationLogsByVerifier(userAId);

        mockMvc.perform(get("/me/verification-logs").cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(userALogs)));
    }

    @Test
    void shouldNotReturnLogsLinkedToOtherUsers() throws Exception {
        SignedFixture fixture = createSignedFixture("others");

        String emailA = uniqueEmail("owner-a");
        String emailB = uniqueEmail("owner-b");
        String password = "Senha@123";
        signup(emailA, password);
        signup(emailB, password);
        Cookie sessionA = loginAndGetSessionCookie(emailA, password);
        Cookie sessionB = loginAndGetSessionCookie(emailB, password);

        mockMvc.perform(get("/verify/{publicId}", fixture.publicId()).cookie(sessionA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/verify/{publicId}", fixture.publicId()).cookie(sessionB))
                .andExpect(status().isOk());

        Long userAId = userIdByEmail(emailA);
        int userALogs = countVerificationLogsByVerifier(userAId);

        mockMvc.perform(get("/me/verification-logs").cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(userALogs)));
    }

    @Test
    void shouldNotReturnAnonymousLogs() throws Exception {
        SignedFixture fixtureA = createSignedFixture("anon-filter-a");
        SignedFixture fixtureB = createSignedFixture("anon-filter-b");

        String email = uniqueEmail("owner");
        String password = "Senha@123";
        signup(email, password);
        Cookie session = loginAndGetSessionCookie(email, password);

        mockMvc.perform(get("/verify/{publicId}", fixtureA.publicId()).cookie(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/verify/{publicId}", fixtureB.publicId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                fixtureA.publicId(),
                                fixtureA.originalText(),
                                fixtureA.signatureBase64())))
                .andExpect(status().isOk());

        Long userId = userIdByEmail(email);
        int userLogs = countVerificationLogsByVerifier(userId);

        mockMvc.perform(get("/me/verification-logs").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(userLogs)));
    }

    @Test
    void shouldReturnEmptyListWhenLoggedInUserHasNoLinkedVerificationLogs() throws Exception {
        SignedFixture fixture = createSignedFixture("empty");

        String emailA = uniqueEmail("empty-owner");
        String emailB = uniqueEmail("empty-other");
        String password = "Senha@123";
        signup(emailA, password);
        signup(emailB, password);
        Cookie sessionA = loginAndGetSessionCookie(emailA, password);
        Cookie sessionB = loginAndGetSessionCookie(emailB, password);

        mockMvc.perform(get("/verify/{publicId}", fixture.publicId()).cookie(sessionB))
                .andExpect(status().isOk());
        mockMvc.perform(get("/verify/{publicId}", fixture.publicId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/me/verification-logs").cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private SignedFixture createSignedFixture(String prefix) throws Exception {
        String signerEmail = uniqueEmail("signer-" + prefix);
        String signerPassword = "Senha@123";
        String text = "texto para verificar " + prefix;

        signup(signerEmail, signerPassword);
        Cookie signerSession = loginAndGetSessionCookie(signerEmail, signerPassword);

        mockMvc.perform(post("/sign")
                        .cookie(signerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload(text)))
                .andExpect(status().isCreated());

        Long signatureId = jdbcTemplate.queryForObject(
                """
                        SELECT s.id
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ? AND s.original_text = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                Long.class,
                signerEmail,
                text
        );
        String signatureBase64 = jdbcTemplate.queryForObject(
                "SELECT signature_base64 FROM signatures WHERE id = ?",
                String.class,
                signatureId
        );
        String publicId = jdbcTemplate.queryForObject(
                "SELECT public_id FROM signatures WHERE id = ?",
                String.class,
                signatureId
        );

        return new SignedFixture(signatureId, publicId, text, signatureBase64);
    }

    private Cookie loginAndGetSessionCookie(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("JSESSIONID");
        assertThat(sessionCookie).isNotNull();
        return sessionCookie;
    }

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, password)))
                .andExpect(status().isCreated());
    }

    private Long userIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }

    private int countVerificationLogsByVerifier(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_logs WHERE verified_by_user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private static String signupPayload(String nome, String email, String password) {
        return """
                {
                  "nome": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(nome, email, password);
    }

    private static String loginPayload(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private static String signPayload(String text) {
        return """
                {
                  "text": "%s"
                }
                """.formatted(text);
    }

    private static String postVerifyPayload(String publicId, String text, String signatureBase64) {
        return """
                {
                  "publicId": "%s",
                  "text": "%s",
                  "signatureBase64": "%s"
                }
                """.formatted(publicId, text, signatureBase64);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "+" + System.nanoTime() + "@example.com";
    }

    private record SignedFixture(Long signatureId, String publicId, String originalText, String signatureBase64) {
    }
}
