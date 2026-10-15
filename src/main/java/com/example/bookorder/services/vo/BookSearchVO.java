package com.example.bookorder.services.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BookSearchVO {

	@PositiveOrZero(message = "Order ID should be a positive or zero value")
	private Long bookId;

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
