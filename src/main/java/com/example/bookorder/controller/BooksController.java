package com.example.bookorder.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.bookorder.common.shared.Constants;
import com.example.bookorder.controller.converter.BookConverter;
import com.example.bookorder.controller.dto.request.BookRequest;
import com.example.bookorder.controller.dto.request.BookSearchRequest;
import com.example.bookorder.controller.dto.response.BookResponse;
import com.example.bookorder.services.BooksService;
import com.example.bookorder.services.vo.BookSearchVO;
import com.example.bookorder.services.vo.BookVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BooksController {

	private final BooksService booksService;
	private final BookConverter bookConverter;

	@PostMapping("/save")
	public String save(@Valid @RequestBody BookRequest request) {
		BookVO bookVO = bookConverter.convert2VO(request);
		return booksService.save(bookVO).toString();
	}
	@PostMapping("/batchSave")
	public String batchSave(@Valid @RequestBody List<BookRequest> request) {
		List<BookVO> bookVOList = new ArrayList<>();
		request.forEach(bookRequest -> bookVOList.add(bookConverter.convert2VO(bookRequest)));
		return booksService.batchSave(bookVOList).toString();
	}

	@DeleteMapping("/{id}")
	public void delete(@Valid @NotNull @PathVariable(Constants.ID) Long id) {
		booksService.delete(id);
	}

	@PutMapping("/{id}")
	public void update(@NotNull @Positive @PathVariable(Constants.ID) Long id,
			@Valid @RequestBody BookRequest request) {
		BookVO booksUpdateVO = bookConverter.convert2VO(request);
		booksService.update(id, booksUpdateVO);
	}

	@GetMapping("/{id}")
	public List<BookResponse> queryById(@Positive @PathVariable(Constants.ID) Long id) {
		return booksService.queryById(id).stream().map(bookConverter::convert2Response).toList();
	}

	@PostMapping("/search")
	public Page<BookVO> queryByCriteria(@Valid @RequestBody BookSearchRequest request) {
		Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize());
		BookSearchVO bookSearchVO = bookConverter.convert2SearchBookVO(request);
		return booksService.queryByCriteria(bookSearchVO, pageable);
	}
}
