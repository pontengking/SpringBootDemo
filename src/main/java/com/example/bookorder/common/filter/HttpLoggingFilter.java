package com.example.bookorder.common.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class HttpLoggingFilter extends OncePerRequestFilter {
	/**
	 * Generate a trace ID and log API outcomes separately from transport status.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		long startTime = System.nanoTime();
		String previousTraceId = MDC.get("traceId");
		String traceId = UUID.randomUUID().toString();
		request.setAttribute("traceId", traceId);
		response.setHeader("X-Trace-Id", traceId);
		MDC.put("traceId", traceId);

		try {
			chain.doFilter(request, response);
		} finally {
			try {
				log.info(
						"HTTP method={} path={} httpStatus={} status={} errorCode={} errorDesc={} traceId={}"
								+ " durationMs={}",
						request.getMethod(), request.getRequestURI(), response.getStatus(),
						request.getAttribute("status"), request.getAttribute("errorCode"),
						request.getAttribute("errorDesc"), traceId, (System.nanoTime() - startTime) / 1_000_000);
			} finally {
				if (previousTraceId == null) {
					MDC.remove("traceId");
				} else {
					MDC.put("traceId", previousTraceId);
				}
			}
		}
	}
}
