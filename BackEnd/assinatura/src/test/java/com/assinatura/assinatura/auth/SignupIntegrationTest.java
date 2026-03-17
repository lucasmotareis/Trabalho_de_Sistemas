package com.assinatura.assinatura.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SignupIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void cleanDatabase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("DELETE FROM verification_logs");
        jdbcTemplate.update("DELETE FROM signatures");
        jdbcTemplate.update("DELETE FROM user_keys");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldCreateUserSuccessfullyWithValidPayload() throws Exception {
        String email = uniqueEmail();
        String payload = signupPayload("Lucas", email, "Senha@123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Lucas"))
                .andExpect(jsonPath("$.email").value(email));

        Integer usersCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );

        assertThat(usersCount).isEqualTo(1);
    }

    @Test
    void shouldPersistHashedPasswordInsteadOfPlaintext() throws Exception {
        String email = uniqueEmail();
        String rawPassword = "Senha@123";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, rawPassword)))
                .andExpect(status().isCreated());

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class,
                email
        );

        assertThat(passwordHash).isNotBlank();
        assertThat(passwordHash).isNotEqualTo(rawPassword);
        assertThat(passwordHash).startsWith("$2");
    }

    @Test
    void shouldCreateAUserKeyLinkedToTheCreatedUser() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, "Senha@123")))
                .andExpect(status().isCreated());

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
        Integer userKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_keys WHERE user_id = ?",
                Integer.class,
                userId
        );
        String publicKey = jdbcTemplate.queryForObject(
                "SELECT public_key FROM user_keys WHERE user_id = ?",
                String.class,
                userId
        );
        String privateKeyEncrypted = jdbcTemplate.queryForObject(
                "SELECT private_key_encrypted FROM user_keys WHERE user_id = ?",
                String.class,
                userId
        );

        assertThat(userKeyCount).isEqualTo(1);
        assertThat(publicKey).isNotBlank();
        assertThat(privateKeyEncrypted).isNotBlank();
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        String payload = signupPayload("Lucas", email, "Senha@123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @MethodSource("invalidPayloadProvider")
    void shouldRejectInvalidPayloadWithMissingOrBlankFields(String payload) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotExposeSensitiveFieldsInTheSignupResponse() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload("Lucas", email, "Senha@123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.privateKeyEncrypted").doesNotExist());
    }

    private static Stream<Arguments> invalidPayloadProvider() {
        return Stream.of(
                Arguments.of("""
                        {"email":"sem-nome@example.com","password":"Senha@123"}
                        """),
                Arguments.of("""
                        {"nome":" ","email":"nome-vazio@example.com","password":"Senha@123"}
                        """),
                Arguments.of("""
                        {"nome":"Sem Email","password":"Senha@123"}
                        """),
                Arguments.of("""
                        {"nome":"Email Vazio","email":" ","password":"Senha@123"}
                        """),
                Arguments.of("""
                        {"nome":"Sem Senha","email":"sem-senha@example.com"}
                        """),
                Arguments.of("""
                        {"nome":"Senha Vazia","email":"senha-vazia@example.com","password":" "}
                        """)
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

    private static String uniqueEmail() {
        return "lucas+" + System.nanoTime() + "@example.com";
    }
}
