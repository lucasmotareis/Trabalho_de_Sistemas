package com.assinatura.assinatura.keys;

import com.assinatura.assinatura.service.crypto.AesGcmKeyEncryptionService;
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

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class MyKeysIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AesGcmKeyEncryptionService keyEncryptionService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcTemplate.update("DELETE FROM verification_logs");
        jdbcTemplate.update("DELETE FROM signatures");
        jdbcTemplate.update("DELETE FROM user_keys");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void shouldRequireAuthenticationForKeysMe() throws Exception {
        mockMvc.perform(get("/keys/me"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void shouldReturnOnlyLoggedInUsersOwnKeys() throws Exception {
        String emailOne = uniqueEmail("owner-a");
        String emailTwo = uniqueEmail("owner-b");
        String password = "Senha@123";
        signup(emailOne, password);
        signup(emailTwo, password);
        Cookie sessionCookieTwo = loginAndGetSessionCookie(emailTwo, password);
        Long userIdTwo = userIdByEmail(emailTwo);
        String publicKeyTwo = publicKeyByEmail(emailTwo);
        String privateKeyTwo = decryptedPrivateKeyByEmail(emailTwo);

        mockMvc.perform(get("/keys/me").cookie(sessionCookieTwo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userIdTwo))
                .andExpect(jsonPath("$.email").value(emailTwo))
                .andExpect(jsonPath("$.publicKey").value(publicKeyTwo))
                .andExpect(jsonPath("$.privateKey").value(privateKeyTwo));
    }

    @Test
    void shouldIncludeExpectedFieldsInKeysMeResponse() throws Exception {
        String email = uniqueEmail("fields");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);

        mockMvc.perform(get("/keys/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.publicKey").isString())
                .andExpect(jsonPath("$.privateKey").isString());
    }

    @Test
    void shouldNotAllowChoosingAnotherUserThroughUserIdQueryParameter() throws Exception {
        String emailOne = uniqueEmail("query-a");
        String emailTwo = uniqueEmail("query-b");
        String password = "Senha@123";
        signup(emailOne, password);
        signup(emailTwo, password);
        Long userIdOne = userIdByEmail(emailOne);
        Long userIdTwo = userIdByEmail(emailTwo);
        Cookie sessionCookieTwo = loginAndGetSessionCookie(emailTwo, password);

        mockMvc.perform(get("/keys/me")
                        .param("userId", userIdOne.toString())
                        .cookie(sessionCookieTwo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userIdTwo))
                .andExpect(jsonPath("$.email").value(emailTwo));
    }

    @Test
    void shouldNeverExposeAnotherUsersPrivateKey() throws Exception {
        String emailOne = uniqueEmail("private-a");
        String emailTwo = uniqueEmail("private-b");
        String password = "Senha@123";
        signup(emailOne, password);
        signup(emailTwo, password);
        Cookie sessionCookieTwo = loginAndGetSessionCookie(emailTwo, password);
        String privateKeyOne = decryptedPrivateKeyByEmail(emailOne);

        mockMvc.perform(get("/keys/me").cookie(sessionCookieTwo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateKey", not(equalTo(privateKeyOne))));
    }

    @Test
    void shouldReturnConflictWhenAuthenticatedUserHasNoKeyPair() throws Exception {
        String email = uniqueEmail("missing-key");
        String password = "Senha@123";
        signup(email, password);
        Cookie sessionCookie = loginAndGetSessionCookie(email, password);
        Long userId = userIdByEmail(email);
        jdbcTemplate.update("DELETE FROM user_keys WHERE user_id = ?", userId);

        mockMvc.perform(get("/keys/me").cookie(sessionCookie))
                .andExpect(status().isConflict());
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

    private String publicKeyByEmail(String email) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT uk.public_key
                        FROM user_keys uk
                        JOIN users u ON u.id = uk.user_id
                        WHERE u.email = ?
                        """,
                String.class,
                email
        );
    }

    private String decryptedPrivateKeyByEmail(String email) {
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
        byte[] privateKeyBytes = keyEncryptionService.decryptPrivateKey(privateKeyEncrypted);
        return Base64.getEncoder().encodeToString(privateKeyBytes);
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

    private static String uniqueEmail(String prefix) {
        return prefix + "+" + System.nanoTime() + "@example.com";
    }
}
