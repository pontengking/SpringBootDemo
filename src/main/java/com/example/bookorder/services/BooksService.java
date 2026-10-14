package com.example.bookorder.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.example.bookorder.common.exception.BizException;
import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.repository.BooksRepository;
import com.example.bookorder.repository.entity.Books;
import com.example.bookorder.services.vo.BookSearchVO;
import com.example.bookorder.services.vo.BookVO;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class BooksService {

	private final BooksRepository booksRepository;

	public Long save(@Valid @NotNull BookVO vO) {
		Books book = new Books();
		BeanUtils.copyProperties(vO, book);
		book = booksRepository.save(book);
		return book.getBookId();
	}

	public List<Long> batchSave(@Valid @NotNull List<BookVO> vOs) {
		List<Books> books = vOs.stream().map(vo -> {
			Books book = new Books();
			BeanUtils.copyProperties(vo, book, "bookId");
			return book;
		}).toList();

		return booksRepository.saveAll(books).stream().map(Books::getBookId).toList();
	}

	public void delete(@NotNull @Positive Long id) {
		booksRepository.deleteById(id);
	}

	/**
	 * Lock and edit in one bounded transaction. Only the explicit update mutates
	 * the row; missing rows and unexpected counts fail and roll back.
	 */
	@Transactional(timeout = 5, rollbackFor = Exception.class)
	public void update(@NotNull @Positive Long id, @NotNull @Valid BookVO request) {
		try {
			// 1. take the row lock and confirm the book exists
			Books book = booksRepository.lockByBookId(id)
					.orElseThrow(() -> new BizException(ErrorCodeEnum.BOOK_NOT_FOUND));

			// 2. do any business checks using the locked data
			if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
				throw new BizException(ErrorCodeEnum.PARAM_INVALID);
			}

			book.setTitle(request.getTitle());
			book.setAuthor(request.getAuthor());
			book.setPrice(request.getPrice());

			if (request.getUnitsInStock() != null) {
				book.setUnitsInStock(request.getUnitsInStock());
			}

		} catch (PessimisticLockingFailureException e) {
			throw new BizException(ErrorCodeEnum.SYSTEM_BUSY, e);
		}
	}

	public List<BookVO> queryById(@NotNull @Positive Long id) {
		return booksRepository.findById(id).map(book -> Collections.singletonList(toVO(book)))
				.orElseGet(Collections::emptyList);
	}

	public Page<BookVO> queryByCriteria(@NotNull @Valid BookSearchVO vo, @NotNull Pageable pageable) {
		if (pageable.isUnpaged() || pageable.getPageSize() > 100 || pageable.getOffset() > Integer.MAX_VALUE) {
			throw new BizException(ErrorCodeEnum.PARAM_INVALID);
		}
		Specification<Books> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (vo.getTitle() != null) {
				predicates.add(builder.equal(root.get("title"), vo.getTitle()));
			}
			if (vo.getAuthor() != null) {
				predicates.add(builder.equal(root.get("author"), vo.getAuthor()));
			}
			if (vo.getMinPrice() != null && vo.getMaxPrice() != null
					&& vo.getMinPrice().compareTo(vo.getMaxPrice()) > 0) {
				predicates.add(builder.between(root.<BigDecimal>get("price"), vo.getMinPrice(), vo.getMaxPrice()));
			}
			return builder.and(predicates.toArray(new Predicate[0]));
		};
		return booksRepository.findAll(specification, pageable).map(this::toVO);
	}

	private BookVO toVO(Books source) {
		BookVO vo = new BookVO();
		BeanUtils.copyProperties(source, vo);
		return vo;
	}

}
