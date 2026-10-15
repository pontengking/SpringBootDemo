package com.example.bookorder.controller.converter;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.example.bookorder.controller.dto.request.BookOrderRequest;
import com.example.bookorder.controller.dto.request.BookOrderSearchRequest;
import com.example.bookorder.controller.dto.response.BookOrderResponse;
import com.example.bookorder.services.vo.BookOrderVO;
import com.example.bookorder.services.vo.BookOrdersSearchVO;

@Component
public class BookOrderConverter {

	public BookOrderVO convert2VO(BookOrderRequest request) {
		BookOrderVO vo = new BookOrderVO();
		BeanUtils.copyProperties(request, vo);
		return vo;
	}

	public BookOrderResponse convert2Response(BookOrderVO source) {
		return new BookOrderResponse(source.getOrderId(), source.getUserId(), source.getOrderType(),
				source.getBookOrderDetailVOs(), source.getTotalAmount());
	}

	/**
	 * Build the service search criteria; a zero id is normalised to null so that no
	 * id predicate is added to the query.
	 */
	public BookOrdersSearchVO convert2SearchBookOrderVO(BookOrderSearchRequest request) {
		BookOrdersSearchVO vo = new BookOrdersSearchVO();
		vo.setOrderId(positiveOrNull(request.getOrderId()));
		vo.setUserId(request.getUserId());
		vo.setOrderType(request.getOrderType());
		vo.setBookId(positiveOrNull(request.getBookId()));
		vo.setStartDate(request.getStartDate());
		vo.setEndDate(request.getEndDate());
		return vo;
	}

	/**
	 * Map the "no filter" sentinel (null or zero) to null; ids are always positive.
	 */
	private Long positiveOrNull(Long id) {
		return id == null || id <= 0L ? null : id;
	}
}
