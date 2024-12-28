package com.timess.picturecloud.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author 33363
 */
@Aspect
@Component
@Slf4j
public class LoggerInterceptor {
    @Before("execution(public * com.timess.picturecloud.controller..*(..))")
    public void logMethodEntry(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();
        log.info("==================================================================================");
        log.info("请求方法: {}.{} 请求参数: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                methodName,
                Arrays.toString(args));
        log.info("==================================================================================");

    }

}
