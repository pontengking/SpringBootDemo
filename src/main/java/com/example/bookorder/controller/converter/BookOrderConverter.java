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

	public BookOrdersSearchVO convert2SearchBookOrderVO(BookOrderSearchRequest request) {
		BookOrdersSearchVO vo = new BookOrdersSearchVO();
		vo.setOrderId(request.getOrderId());
		vo.setUserId(request.getUserId());
		vo.setOrderType(request.getOrderType());
		vo.setBookId(request.getBookId());
		vo.setStartDate(request.getStartDate());
		vo.setEndDate(request.getEndDate());
		return vo;
	}
}
