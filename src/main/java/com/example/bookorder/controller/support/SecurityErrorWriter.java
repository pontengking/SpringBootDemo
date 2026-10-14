package com.example.bookorder.controller.support;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.controller.dto.response.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Security failures happen before controller advice; serialize the same
 * envelope here.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {
	private final ObjectMapper objectMapper;

	/**
	 * Deny access without invoking the controller, while retaining HTTP 200
	 * transport semantics.
	 */
	public void write(HttpServletRequest request, HttpServletResponse response, ErrorCodeEnum error)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		BaseResponse<Void> body = BaseResponse.failure(error, (String) request.getAttribute("traceId"));
		ControllerResponses.recordOutcome(request, body);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
