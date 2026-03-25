package com.assinatura.assinatura.keys;

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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class PublicKeysIntegrationTest {

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
    void shouldReturnHttp200WhenListingPublicKeys() throws Exception {
        signup(uniqueEmail("status"), "Senha@123");

        mockMvc.perform(get("/keys/public"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnAListWhenListingPublicKeys() throws Exception {
        signup(uniqueEmail("list"), "Senha@123");

        mockMvc.perform(get("/keys/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnPublicKeyForEachEntry() throws Exception {
        signup(uniqueEmail("key-a"), "Senha@123");
        signup(uniqueEmail("key-b"), "Senha@123");

        mockMvc.perform(get("/keys/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].publicKey", everyItem(not(isEmptyOrNullString()))));
    }

    @Test
    void shouldNotExposePrivateKeyInPublicListing() throws Exception {
        signup(uniqueEmail("safe"), "Senha@123");

        mockMvc.perform(get("/keys/public"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"privateKey\""))))
                .andExpect(content().string(not(containsString("\"privateKeyEncrypted\""))));
    }

    @Test
    void shouldReturnMultipleUsersCorrectly() throws Exception {
        String emailOne = uniqueEmail("user-one");
        String emailTwo = uniqueEmail("user-two");
        signup(emailOne, "Senha@123");
        signup(emailTwo, "Senha@123");

        Integer userIdOne = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Integer.class,
                emailOne
        );
        Integer userIdTwo = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Integer.class,
                emailTwo
        );

        mockMvc.perform(get("/keys/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", containsInAnyOrder(emailOne, emailTwo)))
                .andExpect(jsonPath("$[*].userId", containsInAnyOrder(userIdOne, userIdTwo)));
    }

    @Test
    void shouldReturnEmptyListWhenNoKeysExist() throws Exception {
        mockMvc.perform(get("/keys/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
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

    private static String uniqueEmail(String prefix) {
        return prefix + "+" + System.nanoTime() + "@example.com";
    }
}
