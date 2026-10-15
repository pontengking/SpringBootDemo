package com.example.bookorder.services.vo;

import java.util.Date;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BookOrdersSearchVO {
	@PositiveOrZero(message = "Order ID should be a positive or zero value")
	private Long orderId;

	@PositiveOrZero(message = "User ID should be a positive or zero value")
	private String userId;

	private String orderType;

	@PositiveOrZero(message = "Book ID should be a positive or zero value")
	private Long bookId;

	private Date startDate;

	private Date endDate;

}
