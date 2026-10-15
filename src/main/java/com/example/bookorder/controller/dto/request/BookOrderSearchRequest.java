package com.example.bookorder.controller.dto.request;

import java.util.Date;

import com.example.bookorder.controller.dto.request.base.PageRequest;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * JSON pagination input for listing orders. A zero or missing id means "no id
 * filter"; negative ids are rejected.
 */
@Getter
@Setter
public class BookOrderSearchRequest extends PageRequest {
	@PositiveOrZero
	private Long orderId;

	@PositiveOrZero
	private Long bookId;

	@Size(max = 32)
	private String userId;

	@Size(max = 32)
	private String orderType;

	private Date startDate;
	private Date endDate;

}
