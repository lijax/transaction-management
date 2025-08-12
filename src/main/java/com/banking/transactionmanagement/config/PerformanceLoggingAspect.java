package com.banking.transactionmanagement.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect for logging performance metrics of service and repository methods
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceLoggingAspect.class);
    private static final long SLOW_EXECUTION_THRESHOLD_MS = 100;

    /**
     * Log execution time for all service methods
     */
    @Around("execution(* com.banking.transactionmanagement.service.*.*(..))")
    public Object logServiceMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Log execution time for all repository methods
     */
    @Around("execution(* com.banking.transactionmanagement.repository.*.*(..))")
    public Object logRepositoryMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }

    /**
     * Log execution time for all controller methods
     */
    @Around("execution(* com.banking.transactionmanagement.controller.*.*(..))")
    public Object logControllerMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "CONTROLLER");
    }

    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        long startTime = System.nanoTime();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.nanoTime() - startTime;
            long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(executionTime);
            
            if (executionTimeMs > SLOW_EXECUTION_THRESHOLD_MS) {
                logger.warn("[{}] SLOW EXECUTION - {}: {}ms", layer, fullMethodName, executionTimeMs);
            } else {
                logger.debug("[{}] EXECUTION - {}: {}ms", layer, fullMethodName, executionTimeMs);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.nanoTime() - startTime;
            long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(executionTime);
            
            logger.error("[{}] ERROR EXECUTION - {}: {}ms - Exception: {}", 
                        layer, fullMethodName, executionTimeMs, e.getMessage());
            throw e;
        }
    }
}