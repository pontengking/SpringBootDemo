package com.example.bookorder.controller.dto.request.base;

import com.example.bookorder.common.logging.JsonLogFormatter;

/**
 * Common parent for JSON request DTOs with inherited JSON logging support.
 */
public abstract class BaseRequest {
	/**
	 * Convert this request, including inherited fields, to a JSON string for
	 * logging.
	 */
	@Override
	public final String toString() {
		return JsonLogFormatter.toJson(this);
	}
}
