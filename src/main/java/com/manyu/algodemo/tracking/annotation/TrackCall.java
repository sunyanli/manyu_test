package com.manyu.algodemo.tracking.annotation;

import com.manyu.algodemo.tracking.model.enums.BizType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 埋点注解：标注在受监控的接口方法上，由 AOP 环绕通知异步记录调用次数与调用人（F06）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackCall {

    /**
     * 业务类型。
     *
     * @return 业务类型
     */
    BizType type();
}
