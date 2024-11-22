package com.github.xioshe.net.channels.common.lock.annotation;

import com.github.xioshe.net.channels.common.lock.config.LockAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(LockAutoConfiguration.class)
public @interface EnableLocking {
}