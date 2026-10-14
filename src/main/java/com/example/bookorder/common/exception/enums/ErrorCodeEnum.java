package com.example.bookorder.common.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stable API error identifiers and safe descriptions, independent of HTTP
 * status codes.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCodeEnum {

	INVALID_REQUEST("INVALID_REQUEST", "Invalid request. Check required fields, values and pagination."),

	NOT_FOUND("NOT_FOUND", "The requested resource was not found."),

	CONFLICT("CONFLICT", "The operation conflicts with existing data."),

	EXTERNAL_UNAVAILABLE("EXTERNAL_UNAVAILABLE", "The external service is unavailable."),

	UNAUTHORIZED("UNAUTHORIZED", "Authentication is required or the credentials are invalid."),

	FORBIDDEN("FORBIDDEN", "Access to this resource is denied."),

	METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "The HTTP method is not supported for this resource."),

	UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "Use application/json for the request body."),

	NOT_ACCEPTABLE("NOT_ACCEPTABLE", "The requested response format is not supported."),

	SYSTEM_BUSY("SYSTEM_BUSY", "The system is busy. Please try again later."),

	INTERNAL_ERROR("INTERNAL_ERROR", "An internal error occurred. Contact support with the trace ID."),

	PARAM_INVALID("PARAM_INVALID", "The request parameters are invalid."),

	// Business Error
	BOOK_NOT_FOUND("BOOK_NOT_FOUND", "The requested book was not found.");

	private final String errorCode;
	private final String errorDesc;
}