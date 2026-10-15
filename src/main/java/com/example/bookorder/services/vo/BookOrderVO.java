package com.example.bookorder.services.vo;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Service-layer request data, independent of HTTP and persistence entities. */
@Getter
@Setter
public class BookOrderVO {
	@PositiveOrZero(message = "Order ID should be a positive or zero value")
	private Long orderId;

	@Size(max = 32, message = "User ID should not be longer than 32 characters")
	private String userId;

	@Size(max = 32, message = "Order type should not be longer than 32 characters")
	private String orderType;

	@Valid
	@NotNull
	private List<BookOrderDetailVO> bookOrderDetailVOs;

	private BigDecimal totalAmount;
}
