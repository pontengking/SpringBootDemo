package com.example.bookorder.controller.dto.request;

import java.math.BigDecimal;

import com.example.bookorder.controller.dto.request.base.PageRequest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * JSON search input; missing or null text searches all books.
 */
@Getter
public class BookSearchRequest extends PageRequest {

	@Size(max = 200)
	private String title;

	@Size(max = 150)
	private String author;

	@DecimalMin("0.01")
	@Digits(integer = 10, fraction = 2)
	private BigDecimal minPrice;

	@DecimalMin("0.01")
	@Digits(integer = 10, fraction = 2)
	private BigDecimal maxPrice;

}
