package com.assinatura.assinatura.signature;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SigningPrivateKeyPreparationIntegrationTest {

    private static final String IMPLEMENTATION_CLASS =
            "com.assinatura.assinatura.service.crypto.AesGcmKeyEncryptionService";

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
    void shouldDecryptPersistedPrivateKeyAndRebuildAValidPrivateKey() throws Exception {
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

        Object encryptionService = instantiateService(validMasterKeyBase64());
        byte[] privateKeyBytes = decryptPrivateKey(encryptionService, privateKeyEncrypted);

        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        assertThat(privateKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
    }

    private static Object instantiateService(String masterKeyBase64) {
        try {
            Class<?> implementation = Class.forName(IMPLEMENTATION_CLASS);
            for (Constructor<?> constructor : implementation.getConstructors()) {
                if (constructor.getParameterCount() == 1
                        && constructor.getParameterTypes()[0].equals(String.class)) {
                    return constructor.newInstance(masterKeyBase64);
                }
            }
            fail("Expected constructor with master key String in " + IMPLEMENTATION_CLASS);
            return null;
        } catch (ClassNotFoundException exception) {
            fail("Missing implementation class: " + IMPLEMENTATION_CLASS);
            return null;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static byte[] decryptPrivateKey(Object service, String payload) {
        try {
            Method method = service.getClass().getMethod("decryptPrivateKey", String.class);
            return (byte[]) method.invoke(service, payload);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String validMasterKeyBase64() {
        return Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
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
        return "signing-prep+" + System.nanoTime() + "@example.com";
    }
}
