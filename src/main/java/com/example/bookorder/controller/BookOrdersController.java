package com.example.bookorder.controller;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.bookorder.controller.converter.BookOrderConverter;
import com.example.bookorder.controller.dto.request.BookOrderSearchRequest;
import com.example.bookorder.controller.dto.response.BaseResponse;
import com.example.bookorder.controller.dto.response.BookOrderResponse;
import com.example.bookorder.services.BookOrdersService;
import com.example.bookorder.services.vo.BookOrderVO;
import com.example.bookorder.services.vo.BookOrdersSearchVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/bookOrders")
public class BookOrdersController {

	private BookOrdersService bookOrdersService;
	private BookOrderConverter bookOrderConverter;

	@PostMapping
	public String save(@Valid @RequestBody List<BookOrderVO> vOs) {
		return bookOrdersService.saveBookOrder(vOs).toString();
	}

	@DeleteMapping("/{id}")
	public void delete(@Valid @NotNull @PathVariable("id") Long id) {
		bookOrdersService.deleteBookOrder(id);
	}

	@GetMapping("/{id}")
	public BaseResponse<List<BookOrderResponse>> getById(@Valid @NotNull @PathVariable("id") Long id) {
		List<BookOrderResponse> responses = bookOrdersService.getBookOrderById(id).stream()
				.map(bookOrderConverter::convert2Response).toList();
		return BaseResponse.success(responses, MDC.get("traceId"));
	}

	@GetMapping
	public Page<BookOrderVO> query(@Valid @NotNull @RequestBody BookOrderSearchRequest request) {
		Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize());
		BookOrdersSearchVO bookSearchVO = bookOrderConverter.convert2SearchBookOrderVO(request);
		return bookOrdersService.queryBookOrdersByCriteria(bookSearchVO, pageable);
	}
}
