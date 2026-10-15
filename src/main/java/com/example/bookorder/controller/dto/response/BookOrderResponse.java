package com.example.bookorder.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.example.bookorder.services.vo.BookOrderDetailVO;

/**
 * HTTP response contract.
 */
public record BookOrderResponse(Long orderId, String userId, String orderType,
		List<BookOrderDetailVO> bookOrderDetailVOs, BigDecimal getTotalAmount) {
}
