package com.example.bookorder.controller.advice;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.bookorder.common.exception.BizException;
import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.controller.dto.response.BaseResponse;
import com.example.bookorder.controller.support.ControllerResponses;
import com.example.bookorder.integration.client.GoogleClient.ExternalUnavailableException;
import com.example.bookorder.services.exception.NotFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Map application failures to stable JSON error codes, independently of HTTP
 * status.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ExceptionHandler {
	private final ControllerResponses responses;

	@org.springframework.web.bind.annotation.ExceptionHandler(BizException.class)
	public BaseResponse<Void> business(BizException exception) {
		return logAndFail("business", exception, exception.getErrorCodeEnum());
	}

	/**
	 * Missing domain objects and unmapped resources share the public NOT_FOUND
	 * code.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler({NotFoundException.class, NoResourceFoundException.class,
			NoHandlerFoundException.class})
	public BaseResponse<Void> missing(Exception exception) {
		return logAndFail("missing", exception, ErrorCodeEnum.NOT_FOUND);
	}

	/**
	 * Reject malformed JSON, invalid values, and binding failures without exposing
	 * input.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler({IllegalArgumentException.class,
			ConstraintViolationException.class, MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public BaseResponse<Void> invalid(Exception exception) {
		return logAndFail("invalid", exception, ErrorCodeEnum.INVALID_REQUEST);
	}

	/**
	 * Service transaction rollback completes before a persistence failure reaches
	 * this handler.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
	public BaseResponse<Void> conflict(Exception exception) {
		return logAndFail("conflict", exception, ErrorCodeEnum.CONFLICT);
	}

	/**
	 * External failures use an API code; no automatic retry is performed here.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(ExternalUnavailableException.class)
	public BaseResponse<Void> external(Exception exception) {
		return logAndFail("external", exception, ErrorCodeEnum.EXTERNAL_UNAVAILABLE);
	}

	/**
	 * Report unsupported HTTP methods through the standard API envelope.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public BaseResponse<Void> method(Exception exception) {
		return logAndFail("method", exception, ErrorCodeEnum.METHOD_NOT_ALLOWED);
	}

	/**
	 * Explain the JSON content-type contract through an API error code.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public BaseResponse<Void> mediaType(Exception exception) {
		return logAndFail("mediaType", exception, ErrorCodeEnum.UNSUPPORTED_MEDIA_TYPE);
	}

	/**
	 * Return a JSON envelope even if the requested representation is not supported.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<BaseResponse<Void>> acceptable(Exception exception) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
				.body(logAndFail("acceptable", exception, ErrorCodeEnum.NOT_ACCEPTABLE));
	}

	/**
	 * Log unexpected exceptions internally; return only a safe description and
	 * trace ID.
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
	public BaseResponse<Void> unexpected(Exception exception) {
		BaseResponse<Void> response = responses.failure(ErrorCodeEnum.INTERNAL_ERROR);
		log.error("Unhandled API failure traceId={}", response.getTraceId(), exception);
		return response;
	}

	/**
	 * Print the full stack trace server-side (with trace ID) while returning only a
	 * stable error code to the caller. Kept separate so client output never leaks
	 * exception messages.
	 */
	private BaseResponse<Void> logAndFail(String kind, Exception exception, ErrorCodeEnum error) {
		log.warn("{} API failure traceId={} code={}", kind, MDC.get("traceId"), error, exception);
		return responses.failure(error);
	}
}
