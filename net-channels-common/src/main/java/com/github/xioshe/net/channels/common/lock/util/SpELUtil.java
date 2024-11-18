package com.github.xioshe.net.channels.common.lock.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class SpELUtil {
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();


    public static String parseExpression(String expression, ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        EvaluationContext context = createContext(point, method);
        return PARSER.parseExpression(expression).getValue(context, String.class);
    }

    private static EvaluationContext createContext(ProceedingJoinPoint point, Method method) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = point.getArgs();
        String[] parameterNames = NAME_DISCOVERER.getParameterNames(method);

        for (int i = 0; i < args.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return context;
    }
}