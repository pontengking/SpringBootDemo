package com.example.bookorder.services.vo;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Service-layer request data, independent of HTTP and persistence entities.
 */
@Getter
@Setter
public class BookVO {
	@Id
	private Long bookId;

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

	@PositiveOrZero
	private Integer unitsInStock;

	private Date createdAt;

	private Date updatedAt;
}
