package com.example.bookorder.services.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Service-layer request data, independent of HTTP and persistence entities. */
@Getter
@Setter
public class BookOrderDetailVO {
	@NotNull
	@Positive
	private Long bookId;

	@Min(1)
	@Max(1000000)
	private int quantity;

}
