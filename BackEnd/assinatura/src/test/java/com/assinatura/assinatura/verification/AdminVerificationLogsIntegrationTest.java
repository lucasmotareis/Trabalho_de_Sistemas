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
class AdminVerificationLogsIntegrationTest {

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
    void adminShouldAccessAndViewAllVerificationLogsIncludingAnonymousAndLinked() throws Exception {
        SignedFixture linkedFixture = createSignedFixture("admin-linked");
        SignedFixture anonymousFixture = createSignedFixture("admin-anonymous");

        String verifierEmail = uniqueEmail("verifier");
        String password = "Senha@123";
        signup(verifierEmail, password);
        Cookie verifierSession = loginAndGetSessionCookie(verifierEmail, password);
        String adminEmail = uniqueEmail("admin");
        signup(adminEmail, password);
        promoteToAdmin(adminEmail);
        Cookie adminSession = loginAndGetSessionCookie(adminEmail, password);

        mockMvc.perform(get("/verify/{publicId}", linkedFixture.publicId()).cookie(verifierSession))
                .andExpect(status().isOk());
        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                anonymousFixture.publicId(),
                                anonymousFixture.originalText(),
                                anonymousFixture.signatureBase64())))
                .andExpect(status().isOk());

        Integer totalLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_logs",
                Integer.class
        );
        Integer anonymousLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_logs WHERE verified_by_user_id IS NULL",
                Integer.class
        );
        Integer linkedLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_logs WHERE verified_by_user_id IS NOT NULL",
                Integer.class
        );

        assertThat(totalLogs).isNotNull();
        assertThat(anonymousLogs).isNotNull();
        assertThat(linkedLogs).isNotNull();
        assertThat(anonymousLogs).isGreaterThan(0);
        assertThat(linkedLogs).isGreaterThan(0);

        mockMvc.perform(get("/verification-logs")
                        .cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(totalLogs)));
    }

    @Test
    void nonAdminAuthenticatedUserCannotAccessVerificationLogsEndpoint() throws Exception {
        String email = uniqueEmail("non-admin");
        String password = "Senha@123";
        signup(email, password);
        Cookie session = loginAndGetSessionCookie(email, password);

        mockMvc.perform(get("/verification-logs").cookie(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessVerificationLogsEndpoint() throws Exception {
        mockMvc.perform(get("/verification-logs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
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

    private void promoteToAdmin(String email) {
        jdbcTemplate.update(
                "UPDATE users SET role = 'ROLE_ADMIN' WHERE email = ?",
                email
        );
    }

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, password)))
                .andExpect(status().isCreated());
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
