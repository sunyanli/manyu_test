package com.example.org.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 组织架构事件监听器
 * 处理调动/离职等事件，异步通知下游系统（审批、权限等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgEventListener {

    @Async
    @EventListener
    public void handleEmployeeTransferred(EmployeeTransferredEvent event) {
        log.info("[OrgEvent] 员工调动事件: employeeId={}, dept {} -> {}, position {} -> {}",
                event.getEmployeeId(), event.getFromDeptId(), event.getToDeptId(),
                event.getFromPosition(), event.getToPosition());
        // TODO: 发送到消息队列，通知审批模块更新审批流节点
    }

    @Async
    @EventListener
    public void handleEmployeeResigned(EmployeeResignedEvent event) {
        log.info("[OrgEvent] 员工离职事件: employeeId={}, resignDate={}",
                event.getEmployeeId(), event.getResignDate());
        // TODO: 发送到消息队列，触发权限回收、账号许可释放
    }
}