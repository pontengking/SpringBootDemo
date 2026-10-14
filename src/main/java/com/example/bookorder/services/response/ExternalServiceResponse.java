package com.example.bookorder.services.response;

/**
 * External service page metadata response carrier.
 */
public record ExternalServiceResponse(int status, String contentType, String content) {
	public ExternalServiceResponse(int status, String contentType) {
		this(status, contentType, null);
	}
}
