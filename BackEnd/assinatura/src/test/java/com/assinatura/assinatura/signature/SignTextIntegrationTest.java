package com.assinatura.assinatura.signature;

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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SignTextIntegrationTest {

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
    void shouldReturn401WhenPostingToSignWithoutSession() throws Exception {
        mockMvc.perform(post("/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload("texto sem sessao")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSignTextSuccessfullyWhenAuthenticated() throws Exception {
        String email = uniqueEmail("sign-ok");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload("texto para assinatura")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.publicId").isString())
                .andExpect(jsonPath("$.signatureBase64").isString())
                .andExpect(jsonPath("$.hashAlgorithm").value("SHA-256"))
                .andExpect(jsonPath("$.signatureAlgorithm").value("SHA256withRSA"))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void shouldPersistSignatureForAuthenticatedUser() throws Exception {
        String email = uniqueEmail("persist");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);
        String text = "assinar este texto";

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload(text)))
                .andExpect(status().isCreated());

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ? AND s.original_text = ?
                        """,
                Integer.class,
                email,
                text
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldReturnSignatureResponseWithoutSensitiveFields() throws Exception {
        String email = uniqueEmail("safe");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload("texto sensivel")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.privateKeyEncrypted").doesNotExist());
    }

    @Test
    void shouldRejectInvalidPayloadWhenTextIsBlank() throws Exception {
        String email = uniqueEmail("blank");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidPayloadWhenTextIsMissing() throws Exception {
        String email = uniqueEmail("missing");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailWhenAuthenticatedUserHasNoUserKey() throws Exception {
        String email = uniqueEmail("nokey");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
        jdbcTemplate.update("DELETE FROM user_keys WHERE user_id = ?", userId);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload("sem chave")))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(409, 422));
    }

    @Test
    void shouldUseAuthenticatedUsersKeyOnly() throws Exception {
        String emailA = uniqueEmail("user-a");
        String emailB = uniqueEmail("user-b");
        String password = "Senha@123";
        signup(emailA, password);
        signup(emailB, password);
        Cookie sessionCookieB = loginAndGetSessionCookie(emailB, password);
        String text = "texto assinado pelo usuario B";

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookieB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload(text)))
                .andExpect(status().isCreated());

        Long userAId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, emailA);
        Long userBId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, emailB);
        Long persistedSignerId = jdbcTemplate.queryForObject(
                """
                        SELECT s.user_id
                        FROM signatures s
                        WHERE s.original_text = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                Long.class,
                text
        );

        assertThat(persistedSignerId).isEqualTo(userBId);
        assertThat(persistedSignerId).isNotEqualTo(userAId);
    }

    @Test
    void shouldPersistTextHash() throws Exception {
        String email = uniqueEmail("hash");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);
        String text = "texto para hash";

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload(text)))
                .andExpect(status().isCreated());

        String textHash = jdbcTemplate.queryForObject(
                """
                        SELECT s.text_hash
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                String.class,
                email
        );

        assertThat(textHash).isNotBlank();
        assertThat(textHash).isNotEqualTo(text);
    }

    @Test
    void shouldPersistSignatureAlgorithmAndHashAlgorithm() throws Exception {
        String email = uniqueEmail("algos");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload("texto algoritmos")))
                .andExpect(status().isCreated());

        String hashAlgorithm = jdbcTemplate.queryForObject(
                """
                        SELECT s.hash_algorithm
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                String.class,
                email
        );
        String signatureAlgorithm = jdbcTemplate.queryForObject(
                """
                        SELECT s.signature_algorithm
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                String.class,
                email
        );

        assertThat(hashAlgorithm).isEqualTo("SHA-256");
        assertThat(signatureAlgorithm).isEqualTo("SHA256withRSA");
    }

    @Test
    void shouldPersistNonEmptySignatureBase64() throws Exception {
        String email = uniqueEmail("sig-base64");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload("texto base64")))
                .andExpect(status().isCreated());

        String signatureBase64 = jdbcTemplate.queryForObject(
                """
                        SELECT s.signature_base64
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                String.class,
                email
        );

        assertThat(signatureBase64).isNotBlank();
        assertThat(Base64.getDecoder().decode(signatureBase64)).isNotEmpty();
    }

    @Test
    void shouldPersistVerifiableSignatureWithStoredPublicKey() throws Exception {
        String email = uniqueEmail("verify");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);
        String text = "texto para verificacao";

        mockMvc.perform(post("/sign")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signPayload(text)))
                .andExpect(status().isCreated());

        String signatureBase64 = jdbcTemplate.queryForObject(
                """
                        SELECT s.signature_base64
                        FROM signatures s
                        JOIN users u ON u.id = s.user_id
                        WHERE u.email = ?
                        ORDER BY s.id DESC
                        LIMIT 1
                        """,
                String.class,
                email
        );
        String publicKeyBase64 = jdbcTemplate.queryForObject(
                """
                        SELECT uk.public_key
                        FROM user_keys uk
                        JOIN users u ON u.id = uk.user_id
                        WHERE u.email = ?
                        """,
                String.class,
                email
        );

        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        java.security.Signature verifier = java.security.Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(text.getBytes(StandardCharsets.UTF_8));
        boolean valid = verifier.verify(Base64.getDecoder().decode(signatureBase64));

        assertThat(valid).isTrue();
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

    private static String uniqueEmail(String prefix) {
        return prefix + "+" + System.nanoTime() + "@example.com";
    }
}
