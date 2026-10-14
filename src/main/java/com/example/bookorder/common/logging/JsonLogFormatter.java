package com.example.bookorder.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Serialize DTOs or maps as JSON for logging without changing HTTP
 * serialization configuration.
 */
public final class JsonLogFormatter {
	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private JsonLogFormatter() {
	}

	/**
	 * Return JSON text, or a safe JSON marker if a property cannot be serialized.
	 * Logging must not fail the business operation or expose serialization
	 * exception details.
	 */
	public static String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException | RuntimeException exception) {
			return "{\"serializationError\":true}";
		}
	}
}
