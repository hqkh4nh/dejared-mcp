package dev.khanh.mcp.dejaredmcp.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cross-cutting logging for all public methods in the {@code service} package.
 * Keeps business logic clean by centralizing INFO/DEBUG logging, timing, and
 * input/output capture in one place.
 *
 * <ul>
 *   <li><b>INFO:</b> method name, arg count, duration, success/failure summary</li>
 *   <li><b>DEBUG:</b> full input args, full output, result length</li>
 * </ul>
 */
@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Around("execution(public * dev.khanh.mcp.dejaredmcp.service.*.*(..))")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("{}.{}: invoked with {} arg(s)", className, methodName, args.length);
        log.debug("{}.{}: args={}", className, methodName, formatArgs(args));

        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            if (result instanceof String s) {
                if (s.startsWith("Error:") || s.startsWith("No ")) {
                    log.info("{}.{}: completed in {}ms (no result or error)", className, methodName, durationMs);
                } else {
                    log.info("{}.{}: completed in {}ms ({} chars)", className, methodName, durationMs, s.length());
                }
                log.debug("{}.{}: output:\n{}", className, methodName, s);
            } else {
                log.info("{}.{}: completed in {}ms", className, methodName, durationMs);
                log.debug("{}.{}: output={}", className, methodName, result);
            }

            return result;
        } catch (Throwable t) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("{}.{}: threw {} in {}ms", className, methodName, t.getClass().getSimpleName(), durationMs);
            log.debug("{}.{}: exception", className, methodName, t);
            throw t;
        }
    }

    private static String formatArgs(Object[] args) {
        if (args.length == 0) return "[]";
        return Arrays.toString(args);
    }
}
