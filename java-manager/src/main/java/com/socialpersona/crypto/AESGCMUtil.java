package com.socialpersona.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加解密工具类（静态方法）
 *
 * ★ 为什么选 AES-256-GCM 而非 AES-128-CBC：
 *   1. GCM 自带完整性校验（防篡改），CBC 需要额外 HMAC
 *   2. GCM 是流式加密，比 CBC 快
 *   3. GCM 是 TLS 1.3 的标准加密模式，业界最佳实践
 *
 * ★ 加密流程：
 *   每次加密生成随机 12 字节 IV（初始化向量）→ 拼在密文前面一起存
 *   输出格式：Base64(IV + 密文)
 *
 * ★ 为什么 IV 不单独存储：
 *   GCM 要求每次加密用不同的 IV，存在密文前面最安全最简单。
 *   12 字节 IV 不会泄露密钥，不损失安全强度。
 */
public class AESGCMUtil {

    /** AES-GCM 认证标签长度（128 bits = 16 bytes），防篡改 */
    private static final int GCM_TAG_LENGTH = 128;

    /** IV（初始化向量）长度：GCM 建议 12 字节 */
    private static final int GCM_IV_LENGTH = 12;

    /** AES 密钥长度：256 bits */
    private static final int AES_KEY_LENGTH = 256;

    /**
     * 生成随机 256 位 AES 密钥
     *
     * ★ 调用时机：用户首次启动时，生成一把"主密钥"存本地文件。
     *   这把主密钥用于加密/解密所有 AI 的 API Key。
     *
     * @return Base64 编码的密钥字符串
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_LENGTH, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成 AES 密钥失败", e);
        }
    }

    /**
     * AES-256-GCM 加密
     *
     * @param plainText 明文（如 API Key）
     * @param key       Base64 编码的密钥（来自 generateKey()）
     * @return Base64 编码的密文（格式：IV + 密文），存数据库
     */
    public static String encrypt(String plainText, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            // 每次加密生成随机 IV —— 同一段明文两次加密得到不同的密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV（12字节）+ 密文 → Base64
            byte[] combined = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * AES-256-GCM 解密
     *
     * @param cipherText Base64 编码的密文（encrypt() 的输出）
     * @param key        Base64 编码的密钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 从密文中提取 IV（前 12 字节）和真实密文（剩余部分）
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败——密钥错误或密文被篡改", e);
        }
    }
}
