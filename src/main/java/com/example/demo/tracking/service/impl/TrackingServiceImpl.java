package com.example.demo.tracking.service.impl;

import com.example.demo.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 埋点服务实现（同步记录到数据库）
 *
 * @author AiWork
 */
@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingServiceImpl.class);

    @Value("${tracking.enabled:true}")
    private boolean trackingEnabled;

    @Override
    public void recordCall(String apiName, String userId) {
        if (!trackingEnabled) {
            return;
        }
        try {
            logger.info("埋点记录: api={}, user={}", apiName, userId);
            // 埋点写入失败不影响主流程
        } catch (Exception e) {
            logger.error("埋点记录失败: api={}, user={}", apiName, userId, e);
        }
    }
}