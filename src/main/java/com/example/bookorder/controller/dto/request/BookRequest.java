package com.example.bookorder.controller.dto.request;

import java.math.BigDecimal;

import com.example.bookorder.controller.dto.request.base.BaseRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * JSON input for creating or updating a book.
 */
@Getter
@Setter
public class BookRequest extends BaseRequest {
	@NotBlank
	@Size(max = 200)
	private String title;

	@NotBlank
	@Size(max = 150)
	private String author;

	@NotNull
	@DecimalMin("0.01")
	@Digits(integer = 10, fraction = 2)
	private BigDecimal price;

	@NotNull
	@PositiveOrZero
	private Integer unitsInStock;
}
