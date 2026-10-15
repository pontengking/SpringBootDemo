package com.example.bookorder.services;

import java.net.http.HttpResponse;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.bookorder.common.exception.BizException;
import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.controller.dto.response.BaseResponse;
import com.example.bookorder.integration.spi.ExternalApiClient;
import com.example.bookorder.services.response.ExternalServiceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class ExternalFetchService {

	/**
	 * Only used as a fallback for single-client / test-fixture (@MockitoBean)
	 * setups.
	 */
	private final List<ExternalApiClient> externalApiClients;

	/**
	 * Resolve the client for the given provider key from the SPI registry and
	 * fetch. Any client failure or HTTP error status is mapped to
	 * BizException(EXTERNAL_UNAVAILABLE).
	 */
	public ExternalServiceResponse fetchExternalPage(String provider) {
		ExternalApiClient client = loadFromRegister(provider);

		HttpResponse<String> response;
		try {
			response = client.fetch();
		} catch (RuntimeException e) {
			log.error("External API client for provider [{}] failed", provider, e);
			throw new BizException(ErrorCodeEnum.EXTERNAL_UNAVAILABLE);
		}

		if (response == null) {
			log.error("External API client for provider [{}] returned null response", provider);
			throw new BizException(ErrorCodeEnum.EXTERNAL_UNAVAILABLE);
		}

		int status = response.statusCode();
		if (status >= 400) {
			log.error("External service for provider [{}] returned HTTP error status: {}", provider, status);
			throw new BizException(ErrorCodeEnum.EXTERNAL_UNAVAILABLE);
		}
		String contentType = response.headers().firstValue("Content-Type").orElse("unknown");
		return new ExternalServiceResponse(status, contentType, response.body());
	}

	/**
	 * Look the client up in the SPI registry (key -> client).
	 */
	public ExternalApiClient loadFromRegister(String provider) {
		ExternalApiClient client = ExternalApiClient.get(provider);
		if (client != null) {
			return client;
		}
		// Single client or test fixture (@MockitoBean) fallback
		if (externalApiClients != null && externalApiClients.size() == 1) {
			return externalApiClients.get(0);
		}
		log.error("No ExternalApiClient registered for provider keyword: {}", provider);
		throw new BizException(ErrorCodeEnum.NOT_FOUND);
	}

	/**
	 * Fetch external page returning BaseResponse envelope with status check and
	 * errorCode/errorDesc mapping.
	 */
	public BaseResponse<ExternalServiceResponse> fetchExternalPageWithBaseResponse(String provider, String traceId) {
		try {
			ExternalServiceResponse response = fetchExternalPage(provider);
			return BaseResponse.success(response, traceId);
		} catch (BizException e) {
			return BaseResponse.failure(e.getErrorCodeEnum(), traceId);
		} catch (Exception e) {
			return BaseResponse.failure(ErrorCodeEnum.EXTERNAL_UNAVAILABLE, traceId);
		}
	}
}