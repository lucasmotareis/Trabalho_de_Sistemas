package com.assinatura.assinatura.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class StatefulLoginSessionIntegrationTest {

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
    void shouldLoginSuccessfullyWithValidEmailAndPassword() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        String email = uniqueEmail();
        signup(email, "Senha@123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, "SenhaErrada@123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("naoexiste@example.com", "Senha@123")))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("invalidLoginPayloadProvider")
    void shouldRejectInvalidPayload(String payload) throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotExposeSensitiveFieldsInLoginResponse() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.privateKeyEncrypted").doesNotExist());
    }

    @Test
    void shouldCreateASessionCookieOnSuccessfulLogin() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(email, password)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JSESSIONID"));
    }

    @Test
    void shouldReturnAuthenticatedUserOnAuthMeWhenSessionExists() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(get("/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.privateKeyEncrypted").doesNotExist());
    }

    @Test
    void shouldReturn401OnAuthMeWithoutSession() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutSuccessfullyWhenAuthenticated() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/auth/logout").cookie(sessionCookie))
                .andExpect(status().isOk());
    }

    @Test
    void shouldInvalidateSessionOnLogout() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/auth/logout").cookie(sessionCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldMakeAuthMeReturn401AfterLogout() throws Exception {
        String email = uniqueEmail();
        String password = "Senha@123";
        signup(email, password);

        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(post("/auth/logout").cookie(sessionCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
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

    private static Stream<Arguments> invalidLoginPayloadProvider() {
        return Stream.of(
                Arguments.of("""
                        {"password":"Senha@123"}
                        """),
                Arguments.of("""
                        {"email":" "}
                        """),
                Arguments.of("""
                        {"email":"usuario@example.com"}
                        """),
                Arguments.of("""
                        {"email":"usuario@example.com","password":" "}
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

    private static String loginPayload(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private static String uniqueEmail() {
        return "login+" + System.nanoTime() + "@example.com";
    }
}
