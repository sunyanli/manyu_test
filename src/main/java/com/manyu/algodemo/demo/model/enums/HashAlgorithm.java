package com.manyu.algodemo.demo.model.enums;

/**
 * 哈希算法类型。
 */
public enum HashAlgorithm {

    /** MD5（非安全用途，演示用）。 */
    MD5,
    /** SHA-256（默认推荐）。 */
    SHA256,
    /** SM3（国密合规场景）。 */
    SM3
}
