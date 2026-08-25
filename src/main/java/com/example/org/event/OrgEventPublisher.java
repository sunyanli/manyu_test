package com.example.org.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 组织架构事件发布器
 */
@Component
@RequiredArgsConstructor
public class OrgEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishEmployeeTransferred(EmployeeTransferredEvent event) {
        publisher.publishEvent(event);
    }

    public void publishEmployeeResigned(EmployeeResignedEvent event) {
        publisher.publishEvent(event);
    }
}