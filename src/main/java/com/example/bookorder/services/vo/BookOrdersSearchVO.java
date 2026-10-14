package com.example.bookorder.services.vo;

import java.util.Date;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BookOrdersSearchVO {
	@Size(max = 32, message = "Order ID should not be longer than 32 characters")
	private Long orderId;

	@Size(max = 32, message = "User ID should not be longer than 32 characters")
	private String userId;

	@Size(max = 32, message = "Order type should not be longer than 32 characters")
	private String orderType;

	@Size(max = 32, message = "Book ID should not be longer than 32 characters")
	private Long bookId;

	private Date startDate;

	private Date endDate;

}
