package com.manyu.algodemo.demo.algorithm;

import org.bouncycastle.crypto.digests.SM3Digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希工具：支持 MD5 / SHA-256 / SM3，统一十六进制小写输出。
 */
public final class HashUtils {

    /** 十六进制字符表。 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HashUtils() {
    }

    /**
     * 按算法类型计算文本哈希。
     *
     * @param text      待哈希文本
     * @param algorithm 算法标识（MD5 / SHA256 / SM3）
     * @return 哈希值（十六进制小写）
     */
    public static String hash(String text, String algorithm) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        byte[] digest = switch (algorithm) {
            case "MD5" -> md5(input);
            case "SHA256" -> sha256(input);
            case "SM3" -> sm3(input);
            default -> throw new IllegalArgumentException("unsupported algorithm: " + algorithm);
        };
        return toHex(digest);
    }

    private static byte[] md5(byte[] input) {
        return digest("MD5", input);
    }

    private static byte[] sha256(byte[] input) {
        return digest("SHA-256", input);
    }

    private static byte[] sm3(byte[] input) {
        SM3Digest digest = new SM3Digest();
        digest.update(input, 0, input.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    private static byte[] digest(String algorithm, byte[] input) {
        try {
            return MessageDigest.getInstance(algorithm).digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 缺少哈希算法提供方: " + algorithm, e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }
}
