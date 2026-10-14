package com.example.bookorder.controller.dto.request.base;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Shared zero-based pagination fields for JSON search requests.
 */
@Getter
@Setter
public abstract class PageRequest extends BaseRequest {
	@Min(0)
	@JsonSetter(nulls = Nulls.SKIP)
	private Integer page = 0;

	@Min(1)
	@Max(100)
	@JsonSetter(nulls = Nulls.SKIP)
	private Integer pageSize = 20;
}
