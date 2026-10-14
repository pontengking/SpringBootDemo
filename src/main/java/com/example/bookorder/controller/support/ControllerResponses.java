package com.example.bookorder.controller.support;

import org.springframework.stereotype.Component;

import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.controller.dto.response.BaseResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Build envelopes and expose API outcomes to request logging separately from
 * HTTP status.
 */
@Component
@RequiredArgsConstructor
public class ControllerResponses {
	private final HttpServletRequest request;

	/**
	 * Wrap a committed service result as a successful API response.
	 */
	public <T> BaseResponse<T> success(T data) {
		BaseResponse<T> response = BaseResponse.success(data, (String) request.getAttribute("traceId"));
		recordOutcome(request, response);
		return response;
	}

	/**
	 * Map a known error code without returning exception messages to the caller.
	 */
	public BaseResponse<Void> failure(ErrorCodeEnum error) {
		BaseResponse<Void> response = BaseResponse.failure(error, (String) request.getAttribute("traceId"));
		recordOutcome(request, response);
		return response;
	}

	/** Shared by MVC advice and pre-controller security failures; request-local only. */
	public static void recordOutcome(HttpServletRequest request, BaseResponse<?> response) {
		request.setAttribute("status", response.getStatus());
		request.setAttribute("errorCode", response.getErrorCode());
		request.setAttribute("errorDesc", response.getErrorDesc());
	}
}
