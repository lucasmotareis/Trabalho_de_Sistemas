package com.assinatura.assinatura.crypto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

class KeyEncryptionServiceContractTest {

    private static final String IMPLEMENTATION_CLASS =
            "com.assinatura.assinatura.service.crypto.AesGcmKeyEncryptionService";

    @Test
    void shouldEncryptPrivateKeyBytesIntoADifferentValueThanRawBase64() throws Exception {
        byte[] privateKeyBytes = generatePrivateKeyBytes();
        String rawBase64 = Base64.getEncoder().encodeToString(privateKeyBytes);

        Object service = instantiateService(validMasterKeyBase64());
        String encrypted = encrypt(service, privateKeyBytes);

        assertThat(encrypted).isNotBlank();
        assertThat(encrypted).isNotEqualTo(rawBase64);
    }

    @Test
    void shouldDecryptEncryptedPayloadBackToTheOriginalBytes() throws Exception {
        byte[] privateKeyBytes = generatePrivateKeyBytes();
        Object service = instantiateService(validMasterKeyBase64());

        String encrypted = encrypt(service, privateKeyBytes);
        byte[] decrypted = decrypt(service, encrypted);

        assertThat(decrypted).isEqualTo(privateKeyBytes);
    }

    @Test
    void shouldProduceDifferentEncryptedOutputsForTheSameInputBecauseOfRandomIv() throws Exception {
        byte[] privateKeyBytes = generatePrivateKeyBytes();
        Object service = instantiateService(validMasterKeyBase64());

        String encryptedOne = encrypt(service, privateKeyBytes);
        String encryptedTwo = encrypt(service, privateKeyBytes);

        assertThat(encryptedOne).isNotEqualTo(encryptedTwo);
    }

    @Test
    void shouldFailWhenMasterKeyIsInvalidLength() {
        assertThatThrownBy(() -> {
            Object service = instantiateService(invalidMasterKeyBase64());
            encrypt(service, generatePrivateKeyBytes());
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldFailWhenPayloadIsTamperedWith() throws Exception {
        byte[] privateKeyBytes = generatePrivateKeyBytes();
        Object service = instantiateService(validMasterKeyBase64());
        String encrypted = encrypt(service, privateKeyBytes);

        String tampered = tamperPayload(encrypted);

        assertThatThrownBy(() -> decrypt(service, tampered))
                .isInstanceOf(RuntimeException.class);
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

    private static String encrypt(Object service, byte[] privateKeyBytes) {
        return (String) invoke(service, "encryptPrivateKey", byte[].class, privateKeyBytes);
    }

    private static byte[] decrypt(Object service, String payload) {
        return (byte[]) invoke(service, "decryptPrivateKey", String.class, payload);
    }

    private static Object invoke(Object target, String methodName, Class<?> parameterType, Object value) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            return method.invoke(target, value);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static byte[] generatePrivateKeyBytes() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair.getPrivate().getEncoded();
    }

    private static String validMasterKeyBase64() {
        return Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    }

    private static String invalidMasterKeyBase64() {
        return Base64.getEncoder().encodeToString("short-key".getBytes());
    }

    private static String tamperPayload(String payload) {
        char[] chars = payload.toCharArray();
        int index = chars.length - 2;
        chars[index] = chars[index] == 'A' ? 'B' : 'A';
        return new String(chars);
    }
}
