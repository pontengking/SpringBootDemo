package com.example.bookorder.controller.dto.request;

import com.example.bookorder.controller.dto.request.base.BaseRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * JSON input for creating an order.
 */
@Getter
@Setter
public class BookOrderRequest extends BaseRequest {
	@NotNull
	@Positive
	private Long bookId;

	@Min(1)
	@Max(1000)
	private int quantity;

	@NotEmpty()
	private Long userId;

}
