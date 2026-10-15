package com.example.bookorder.services.vo;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/** Service-layer request data, independent of HTTP and persistence entities. */
@Getter
@Setter
public class BookOrderDetailVO {
	@PositiveOrZero(message = "Book ID should be a positive or zero value")
	private Long bookId;

	@Positive(message = "Quantity should be a positive value")
	private int quantity;

}
