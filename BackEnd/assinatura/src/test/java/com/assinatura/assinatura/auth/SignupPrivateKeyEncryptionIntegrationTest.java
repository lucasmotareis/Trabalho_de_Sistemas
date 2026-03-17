package com.assinatura.assinatura.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SignupPrivateKeyEncryptionIntegrationTest {

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
    void shouldPersistEncryptedPrivateKeyInsteadOfRawBase64PrivateKey() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, "Senha@123")))
                .andExpect(status().isCreated());

        String privateKeyEncrypted = jdbcTemplate.queryForObject(
                """
                        SELECT uk.private_key_encrypted
                        FROM user_keys uk
                        JOIN users u ON u.id = uk.user_id
                        WHERE u.email = ?
                        """,
                String.class,
                email
        );

        byte[] storedBytes = Base64.getDecoder().decode(privateKeyEncrypted);

        assertThatThrownBy(() -> KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(storedBytes)))
                .isInstanceOf(InvalidKeySpecException.class);
    }

    @Test
    void shouldStillPersistPublicKey() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, "Senha@123")))
                .andExpect(status().isCreated());

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

        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        assertThat(publicKey).isNotNull();
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void shouldCreateAUserKeyLinkedToTheCreatedUser() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, "Senha@123")))
                .andExpect(status().isCreated());

        Integer linkedUserKeyCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_keys uk
                        JOIN users u ON u.id = uk.user_id
                        WHERE u.email = ?
                        """,
                Integer.class,
                email
        );

        assertThat(linkedUserKeyCount).isEqualTo(1);
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

    private static String uniqueEmail() {
        return "private-key+" + System.nanoTime() + "@example.com";
    }
}
