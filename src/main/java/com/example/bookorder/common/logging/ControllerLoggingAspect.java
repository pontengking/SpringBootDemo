package com.example.bookorder.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.example.bookorder.controller.dto.request.base.BaseRequest;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {
	/**
	 * Log controller entry, exit, and elapsed time even when execution fails.
	 */
	@Around("within(com.example.bookorder.controller..*)")
	public Object trace(ProceedingJoinPoint call) throws Throwable {
		long start = System.currentTimeMillis();
		log.debug("Controller request {}", call.getSignature().toShortString());
		if (log.isDebugEnabled()) {
			for (Object argument : call.getArgs()) {
				if (argument instanceof BaseRequest request) {
					String text = request.toString().replace('\r', ' ').replace('\n', ' ');
					String boundedText = text.substring(0, Math.min(text.length(), 2048));
					log.debug("Request traceId={} payload={}", MDC.get("traceId"), boundedText);
				}
			}
		}
		try {
			Object result = call.proceed();
			log.debug("Controller response {}", call.getSignature().toShortString());
			return result;
		} finally {
			log.debug("Controller completed {} durationMs={}", call.getSignature().toShortString(),
					(System.nanoTime() - start) / 1_000_000);
		}
	}
}
