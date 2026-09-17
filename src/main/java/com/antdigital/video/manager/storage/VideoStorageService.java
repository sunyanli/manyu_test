package com.antdigital.video.manager.storage;

/**
 * 视频对象存储：负责结果入库与访问 URL 签发。
 */
public interface VideoStorageService {

    /**
     * 根据对象 key 生成带鉴权限定的访问 URL。
     */
    String buildAccessUrl(String objectKey);
}