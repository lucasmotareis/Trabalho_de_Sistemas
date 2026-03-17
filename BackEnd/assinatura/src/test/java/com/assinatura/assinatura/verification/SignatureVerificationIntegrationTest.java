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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SignatureVerificationIntegrationTest {

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
    void shouldAllowPublicAccessToGetVerifyById() throws Exception {
        SignedFixture signedFixture = createSignedFixture("public-get");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldVerifyStoredSignatureSuccessfullyById() throws Exception {
        SignedFixture signedFixture = createSignedFixture("verify-by-id");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldReturnMetadataForValidStoredSignature() throws Exception {
        SignedFixture signedFixture = createSignedFixture("metadata");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.publicId").value(signedFixture.publicId()))
                .andExpect(jsonPath("$.signerName").isString())
                .andExpect(jsonPath("$.hashAlgorithm").value("SHA-256"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("SHA256withRSA"))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void shouldReturn404WhenSignatureIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/verify/{publicId}", "6cd297f3-68bd-4cea-a568-6ab4f6020336"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPersistVerificationLogWhenVerifyingById() throws Exception {
        SignedFixture signedFixture = createSignedFixture("verify-log-by-id");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_logs WHERE signature_id = ?",
                Integer.class,
                signedFixture.signatureId()
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldAllowPublicAccessToPostVerify() throws Exception {
        SignedFixture signedFixture = createSignedFixture("public-post");

        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText(),
                                signedFixture.signatureBase64())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldReturnInvalidWhenTextIsAltered() throws Exception {
        SignedFixture signedFixture = createSignedFixture("altered-text");

        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText() + " alterado",
                                signedFixture.signatureBase64())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void shouldReturnInvalidWhenSignatureIsAltered() throws Exception {
        SignedFixture signedFixture = createSignedFixture("altered-signature");
        String alteredSignature = signedFixture.signatureBase64().substring(0, signedFixture.signatureBase64().length() - 2) + "AA";

        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText(),
                                alteredSignature)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void shouldRejectInvalidPayloadForPostVerify() throws Exception {
        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPersistVerificationLogWhenVerifyingManually() throws Exception {
        SignedFixture signedFixture = createSignedFixture("verify-log-post");

        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText(),
                                signedFixture.signatureBase64())))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification_logs WHERE signature_id = ?",
                Integer.class,
                signedFixture.signatureId()
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotRequireSessionForVerification() throws Exception {
        SignedFixture signedFixture = createSignedFixture("no-session");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldNotExposeSensitiveFieldsInVerificationResponse() throws Exception {
        SignedFixture signedFixture = createSignedFixture("safe-response");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.privateKeyEncrypted").doesNotExist());
    }

    private SignedFixture createSignedFixture(String prefix) throws Exception {
        String email = uniqueEmail(prefix);
        String password = "Senha@123";
        String text = "texto para verificar " + prefix;

        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
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
                email,
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
