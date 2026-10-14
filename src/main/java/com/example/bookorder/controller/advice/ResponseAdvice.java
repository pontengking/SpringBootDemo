package com.example.bookorder.controller.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.example.bookorder.common.shared.Constants;
import com.example.bookorder.controller.dto.response.BaseResponse;
import com.example.bookorder.controller.support.ControllerResponses;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** Wrap application JSON results after service transactions have completed. */
@RestControllerAdvice(basePackages = "com.example.bookorder.controller")
@RequiredArgsConstructor
public class ResponseAdvice implements ResponseBodyAdvice<Object> {
	private final ObjectMapper objectMapper;

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		// Binary/resource converters and MVC's streaming return handlers retain their
		// own contracts.
		return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)
				|| StringHttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
			Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
		BaseResponse<?> envelope = body instanceof BaseResponse<?> existing
				? existing
				: BaseResponse.success(body, (String) servletRequest.getAttribute(Constants.TRACE_ID));
		ControllerResponses.recordOutcome(servletRequest, envelope);
		response.setStatusCode(HttpStatus.OK);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		// An explicit length for the original body no longer describes the envelope.
		response.getHeaders().remove("Content-Length");
		if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
			try {
				return objectMapper.writeValueAsString(envelope);
			} catch (JsonProcessingException exception) {
				throw new HttpMessageNotWritableException("Cannot serialize API response", exception);
			}
		}
		return envelope;
	}
}
