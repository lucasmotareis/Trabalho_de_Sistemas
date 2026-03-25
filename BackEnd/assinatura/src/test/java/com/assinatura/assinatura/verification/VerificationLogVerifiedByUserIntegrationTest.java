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

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class VerificationLogVerifiedByUserIntegrationTest {

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
    void shouldLinkVerificationLogToAuthenticatedUserWhenVerifyingByPublicId() throws Exception {
        SignedFixture signedFixture = createSignedFixture("auth-get");

        String verifierEmail = uniqueEmail("verifier-get");
        String verifierPassword = "Senha@123";
        signup(verifierEmail, verifierPassword);
        Cookie verifierSession = loginAndGetSessionCookie(verifierEmail, verifierPassword);
        Long verifierId = userIdByEmail(verifierEmail);

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()).cookie(verifierSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        Long verifiedByUserId = latestVerifiedByUserId(signedFixture.signatureId());
        assertThat(verifiedByUserId).isEqualTo(verifierId);
    }

    @Test
    void shouldKeepVerificationLogAnonymousWhenVerifyingByPublicIdWithoutSession() throws Exception {
        SignedFixture signedFixture = createSignedFixture("anon-get");

        mockMvc.perform(get("/verify/{publicId}", signedFixture.publicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        Long verifiedByUserId = latestVerifiedByUserId(signedFixture.signatureId());
        assertThat(verifiedByUserId).isNull();
    }

    @Test
    void shouldLinkVerificationLogToAuthenticatedUserWhenVerifyingManually() throws Exception {
        SignedFixture signedFixture = createSignedFixture("auth-post");

        String verifierEmail = uniqueEmail("verifier-post");
        String verifierPassword = "Senha@123";
        signup(verifierEmail, verifierPassword);
        Cookie verifierSession = loginAndGetSessionCookie(verifierEmail, verifierPassword);
        Long verifierId = userIdByEmail(verifierEmail);

        mockMvc.perform(post("/verify")
                        .cookie(verifierSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText(),
                                signedFixture.signatureBase64())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        Long verifiedByUserId = latestVerifiedByUserId(signedFixture.signatureId());
        assertThat(verifiedByUserId).isEqualTo(verifierId);
    }

    @Test
    void shouldKeepVerificationLogAnonymousWhenVerifyingManuallyWithoutSession() throws Exception {
        SignedFixture signedFixture = createSignedFixture("anon-post");

        mockMvc.perform(post("/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText(),
                                signedFixture.signatureBase64())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        Long verifiedByUserId = latestVerifiedByUserId(signedFixture.signatureId());
        assertThat(verifiedByUserId).isNull();
    }

    @Test
    void shouldPersistExistingVerificationLogFieldsAlongsideVerifierLink() throws Exception {
        SignedFixture signedFixture = createSignedFixture("persist-fields");

        String verifierEmail = uniqueEmail("verifier-fields");
        String verifierPassword = "Senha@123";
        signup(verifierEmail, verifierPassword);
        Cookie verifierSession = loginAndGetSessionCookie(verifierEmail, verifierPassword);
        Long verifierId = userIdByEmail(verifierEmail);

        mockMvc.perform(post("/verify")
                        .cookie(verifierSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postVerifyPayload(
                                signedFixture.publicId(),
                                signedFixture.originalText(),
                                signedFixture.signatureBase64())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        VerificationLogRow row = latestLogRow(signedFixture.signatureId());
        assertThat(row.signatureId()).isEqualTo(signedFixture.signatureId());
        assertThat(row.verifiedByUserId()).isEqualTo(verifierId);
        assertThat(row.providedText()).isEqualTo(signedFixture.originalText());
        assertThat(row.providedSignatureBase64()).isEqualTo(signedFixture.signatureBase64());
        assertThat(row.valid()).isTrue();
        assertThat(row.message()).isEqualTo("Signature is valid");
        assertThat(row.verifiedAt()).isNotNull();
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

    private Long latestVerifiedByUserId(Long signatureId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT verified_by_user_id
                        FROM verification_logs
                        WHERE signature_id = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                signatureId
        );
    }

    private VerificationLogRow latestLogRow(Long signatureId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT signature_id,
                               verified_by_user_id,
                               provided_text,
                               provided_signature_base64,
                               valid,
                               message,
                               verified_at
                        FROM verification_logs
                        WHERE signature_id = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                (resultSet, rowNum) -> new VerificationLogRow(
                        resultSet.getLong("signature_id"),
                        (Long) resultSet.getObject("verified_by_user_id"),
                        resultSet.getString("provided_text"),
                        resultSet.getString("provided_signature_base64"),
                        resultSet.getBoolean("valid"),
                        resultSet.getString("message"),
                        resultSet.getTimestamp("verified_at")
                ),
                signatureId
        );
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

    private record VerificationLogRow(Long signatureId,
                                      Long verifiedByUserId,
                                      String providedText,
                                      String providedSignatureBase64,
                                      boolean valid,
                                      String message,
                                      Timestamp verifiedAt) {
    }
}
