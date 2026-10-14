package com.example.bookorder.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * External page HTTP metadata response contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExternalResponse(int status, String contentType, String content) {
	public ExternalResponse(int status, String contentType) {
		this(status, contentType, null);
	}
}
