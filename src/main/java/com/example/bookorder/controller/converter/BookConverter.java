package com.example.bookorder.controller.converter;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.example.bookorder.controller.dto.request.BookRequest;
import com.example.bookorder.controller.dto.request.BookSearchRequest;
import com.example.bookorder.controller.dto.response.BookResponse;
import com.example.bookorder.services.vo.BookSearchVO;
import com.example.bookorder.services.vo.BookVO;

@Component
public class BookConverter {

	/**
	 * Copy HTTP fields into a separate service request without trimming or
	 * defaulting.
	 */
	public BookVO convert2VO(BookRequest request) {
		BookVO vo = new BookVO();
		BeanUtils.copyProperties(request, vo);
		return vo;
	}

	public BookSearchVO convert2SearchBookVO(BookSearchRequest request) {
		BookSearchVO vo = new BookSearchVO();
		vo.setTitle(request.getTitle());
		vo.setAuthor(request.getAuthor());
		vo.setMinPrice(request.getMinPrice());
		vo.setMaxPrice(request.getMaxPrice());
		return vo;
	}

	public BookResponse convert2Response(BookVO source) {
		return new BookResponse(source.getBookId(), source.getTitle(), source.getAuthor(), source.getPrice(),
				source.getUnitsInStock());

	}
}
