package com.example.bookorder.controller.dto.response;

import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.common.logging.JsonLogFormatter;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * Common JSON envelope. Clients must inspect status even when HTTP status is
 * 200.
 */
@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public final class BaseResponse<T> {
	private final String status;
	private final String errorCode;
	private final String errorDesc;
	private final T data;
	private final String traceId;

	private BaseResponse(String status, String errorCode, String errorDesc, T data, String traceId) {
		this.status = status;
		this.errorCode = errorCode;
		this.errorDesc = errorDesc;
		this.data = data;
		this.traceId = traceId;
	}

	/**
	 * Successful calls contain data and no error details.
	 */
	public static <T> BaseResponse<T> success(T data, String traceId) {
		return new BaseResponse<>("SUCCESS", null, null, data, traceId);
	}

	/**
	 * Failed calls contain a stable public error description and never expose
	 * internal data.
	 */
	public static <T> BaseResponse<T> failure(ErrorCodeEnum error, String traceId) {
		return new BaseResponse<>("FAIL", error.getErrorCode(), error.getErrorDesc(), null, traceId);
	}

	/**
	 * Convert the complete response envelope, including data, to JSON text for
	 * logging.
	 */
	@Override
	public String toString() {
		return JsonLogFormatter.toJson(this);
	}
}
