package com.github.xioshe.net.channels.common.lock.config;

import com.github.xioshe.net.channels.common.lock.annotation.EnableLocking;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class LockingEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String propertyValue = context.getEnvironment().getProperty("app.lock.enabled");
        if ("true".equalsIgnoreCase(propertyValue)) {
            return true;
        }

        return context.getBeanFactory().getBeanNamesForAnnotation(EnableLocking.class).length > 0;
    }
}