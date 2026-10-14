package com.example.bookorder.services.vo;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Service-layer request data, independent of HTTP and persistence entities. */
@Getter
@Setter
public class BookOrderVO {
	@NotNull
	@Positive
	private Long orderId;

	@NotNull
	@Positive
	private Long userId;

	@NotNull
	@NotEmpty
	private String orderType;

	@Valid
	@NotNull
	private List<BookOrderDetailVO> bookOrderDetailVOs;

	private BigDecimal totalAmount;
}
