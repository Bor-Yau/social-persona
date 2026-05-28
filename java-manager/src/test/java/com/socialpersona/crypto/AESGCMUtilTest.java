package com.socialpersona.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AESGCMUtil 单元测试 —— 验证 AES-256-GCM 加解密完整流程
 *
 * 覆盖场景：
 *   1. 正常加解密往返
 *   2. 密钥生成格式验证
 *   3. 同一明文两次加密产生不同密文（IV 随机）
 *   4. 错误密钥解密应失败
 *   5. 中文/特殊字符加解密
 *   6. 空字符串加解密
 *   7. 长文本加解密
 */
public class AESGCMUtilTest {

    /**
     * 标准加解密往返：加密 → 解密 → 明文一致
     */
    @Test
    public void testEncryptDecryptRoundTrip() {
        String key = AESGCMUtil.generateKey();
        String plainText = "sk-test-api-key-12345";

        String cipherText = AESGCMUtil.encrypt(plainText, key);
        assertNotNull(cipherText);
        assertNotEquals(plainText, cipherText, "密文不应等于明文");

        String decrypted = AESGCMUtil.decrypt(cipherText, key);
        assertEquals(plainText, decrypted, "解密后应与明文一致");
    }

    /**
     * generateKey 应返回有效的 Base64 编码密钥
     */
    @Test
    public void testGenerateKeyFormat() {
        String key = AESGCMUtil.generateKey();

        assertNotNull(key);
        assertFalse(key.isEmpty(), "密钥不应为空");
        // Base64 编码的 256-bit 密钥 = 44 字符（32 字节 × 4/3 ≈ 44）
        assertTrue(key.length() >= 40, "Base64 编码的 256 位密钥至少 40 字符");
    }

    /**
     * 每次 generateKey 生成的密钥应不同
     */
    @Test
    public void testGenerateKeyProducesDifferentKeys() {
        String key1 = AESGCMUtil.generateKey();
        String key2 = AESGCMUtil.generateKey();

        assertNotEquals(key1, key2, "每次生成的密钥应不同");
    }

    /**
     * 同一明文用同一密钥加密两次，密文应不同（GCM 的随机 IV 保证）
     */
    @Test
    public void testSamePlainTextProducesDifferentCipherText() {
        String key = AESGCMUtil.generateKey();
        String plainText = "my-secret-api-key";

        String cipher1 = AESGCMUtil.encrypt(plainText, key);
        String cipher2 = AESGCMUtil.encrypt(plainText, key);

        assertNotEquals(cipher1, cipher2, "相同明文应产生不同密文（随机 IV）");
    }

    /**
     * 用错误密钥解密 → 应抛出异常
     */
    @Test
    public void testDecryptWithWrongKeyThrowsException() {
        String key1 = AESGCMUtil.generateKey();
        String key2 = AESGCMUtil.generateKey();
        String plainText = "test-api-key";

        String cipherText = AESGCMUtil.encrypt(plainText, key1);

        assertThrows(RuntimeException.class, () -> {
            AESGCMUtil.decrypt(cipherText, key2);
        }, "错误密钥解密应抛出异常");
    }

    /**
     * 中文文本加解密
     */
    @Test
    public void testChineseTextRoundTrip() {
        String key = AESGCMUtil.generateKey();
        String plainText = "这是一段包含中文的API密钥测试文本。你好世界！";

        String cipherText = AESGCMUtil.encrypt(plainText, key);
        String decrypted = AESGCMUtil.decrypt(cipherText, key);

        assertEquals(plainText, decrypted);
    }

    /**
     * 空字符串加解密
     */
    @Test
    public void testEmptyStringRoundTrip() {
        String key = AESGCMUtil.generateKey();

        String cipherText = AESGCMUtil.encrypt("", key);
        assertNotNull(cipherText);

        String decrypted = AESGCMUtil.decrypt(cipherText, key);
        assertEquals("", decrypted);
    }

    /**
     * 特殊字符加解密
     */
    @Test
    public void testSpecialCharactersRoundTrip() {
        String key = AESGCMUtil.generateKey();
        String plainText = "key!@#$%^&*()_+-=[]{}|;:',.<>?/~`\n\t\\\"";

        String cipherText = AESGCMUtil.encrypt(plainText, key);
        String decrypted = AESGCMUtil.decrypt(cipherText, key);

        assertEquals(plainText, decrypted);
    }

    /**
     * 长文本加解密
     */
    @Test
    public void testLongTextRoundTrip() {
        String key = AESGCMUtil.generateKey();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("LongApiKeyValue_");
        }
        String plainText = sb.toString();

        String cipherText = AESGCMUtil.encrypt(plainText, key);
        String decrypted = AESGCMUtil.decrypt(cipherText, key);

        assertEquals(plainText, decrypted);
    }

    /**
     * 篡改密文 → 解密失败
     */
    @Test
    public void testTamperedCipherTextThrowsException() {
        String key = AESGCMUtil.generateKey();
        String plainText = "secret";

        String cipherText = AESGCMUtil.encrypt(plainText, key);
        String tampered = cipherText.substring(0, cipherText.length() - 1) + "X";

        assertThrows(RuntimeException.class, () -> {
            AESGCMUtil.decrypt(tampered, key);
        }, "篡改过的密文应解密失败");
    }

    /**
     * 无效的密钥格式 → 加密应失败
     */
    @Test
    public void testEncryptWithInvalidKeyThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            AESGCMUtil.encrypt("test", "not-a-valid-base64-key!!!");
        }, "无效密钥应抛出异常");
    }
}