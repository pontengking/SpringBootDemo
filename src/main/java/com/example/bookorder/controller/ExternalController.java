package com.example.bookorder.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookorder.controller.converter.ExternalApiConverter;
import com.example.bookorder.controller.dto.response.ExternalResponse;
import com.example.bookorder.services.ExternalFetchService;
import com.example.bookorder.services.response.ExternalServiceResponse;

/**
 * External API: fetch page metadata through the service integration boundary.
 * Request headers: Authorization: Basic credentials; Content-Type:
 * application/json for JSON bodies. Response headers: Content-Type:
 * application/json and server-generated X-Trace-Id. Handled outcomes use HTTP
 * 200; inspect BaseResponse status and errorCode.
 */
@Validated
@RestController
@RequestMapping({"/api/external", "/external"})
public class ExternalController {

	private ExternalFetchService externalFetchService;
	private ExternalApiConverter converter;

	/**
	 * Fetch external page metadata through the business layer and integration SPI.
	 */
	@GetMapping(value = "/google", produces = MediaType.APPLICATION_JSON_VALUE)
	public ExternalResponse fetchGooglePage() {
		ExternalServiceResponse page = externalFetchService.fetchExternalPage("google");
		return converter.externalServiceResponse2ExternalResponse(page);
	}
}
