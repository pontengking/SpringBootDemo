package com.example.bookorder.controller.converter;

import org.springframework.stereotype.Component;

import com.example.bookorder.controller.dto.response.ExternalResponse;
import com.example.bookorder.services.response.ExternalServiceResponse;

@Component
public class ExternalApiConverter {

	public ExternalResponse externalServiceResponse2ExternalResponse(ExternalServiceResponse source) {
		if (source == null) {
			return null;
		}
		return new ExternalResponse(source.status(), source.contentType(), source.content());
	}
}
