package com.antdigital.video.manager.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 本地/OSS 兼容的默认存储实现：以可选公网前缀拼接访问 URL。
 * 真实环境可替换为 OSS 与 MinIO 的兼容实现并做签名。
 */
@Component
public class DefaultVideoStorageService implements VideoStorageService {

    @Value("${video.storage.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String buildAccessUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("对象 key 不能为空");
        }
        return baseUrl + "/videos/" + objectKey;
    }
}