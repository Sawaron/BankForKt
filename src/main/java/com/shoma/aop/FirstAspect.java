package com.shoma.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Slf4j
@Aspect
@Component
public class FirstAspect {

    @Pointcut("execution(* com.shoma.controller.BankController.*(..))")
    public void bankControllerMethods(){}

    @Before("bankControllerMethods()")
    public void logBeforeOperation(JoinPoint joinPoint){
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info("---> [INFO] Вызван метод: {} с параметрами: {}", methodName, Arrays.toString(args));
    }
}
