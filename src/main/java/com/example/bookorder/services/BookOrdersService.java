package com.example.bookorder.services;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookorder.common.exception.BizException;
import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.repository.BookOrderItemsRepository;
import com.example.bookorder.repository.BookOrdersRepository;
import com.example.bookorder.repository.BooksRepository;
import com.example.bookorder.repository.entity.BookOrderItems;
import com.example.bookorder.repository.entity.BookOrders;
import com.example.bookorder.repository.entity.Books;
import com.example.bookorder.services.vo.BookOrderDetailVO;
import com.example.bookorder.services.vo.BookOrderVO;
import com.example.bookorder.services.vo.BookOrdersSearchVO;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookOrdersService {

	private final BookOrdersRepository bookOrdersRepository;
	private final BookOrderItemsRepository bookOrderItemsRepository;
	private final BooksRepository booksRepository;

	@Transactional(timeout = 5, rollbackFor = Exception.class)
	public Long saveBookOrder(BookOrderVO vO) {
		BookOrders order = new BookOrders();
		order.setUserId(String.valueOf(vO.getUserId()));
		order.setOrderType(vO.getOrderType());

		// Build the line items and accumulate the order total
		BigDecimal totalAmount = BigDecimal.ZERO;
		List<BookOrderItems> bookOrderItems = new ArrayList<>();
		for (BookOrderDetailVO vo : vO.getBookOrderDetailVOs()) {
			Books book = booksRepository.findById(vo.getBookId())
					.orElseThrow(() -> new BizException(ErrorCodeEnum.BOOK_NOT_FOUND));
			BookOrderItems bookOrderItem = new BookOrderItems();
			bookOrderItem.setBookId(book);
			bookOrderItem.setQuantity(vo.getQuantity());
			bookOrderItem.setUnitPrice(book.getPrice());
			bookOrderItem.setItemTotal(book.getPrice().multiply(BigDecimal.valueOf(vo.getQuantity())));
			bookOrderItems.add(bookOrderItem);
			totalAmount = totalAmount.add(bookOrderItem.getItemTotal());
		}
		order.setTotalAmount(totalAmount);

		// No JPA association: write the parent first to obtain the generated order_id,
		// then stamp it onto each item and write the second table. The DB foreign key
		// still enforces that every item references an existing order.
		Long orderId = bookOrdersRepository.save(order).getOrderId();
		bookOrderItems.forEach(item -> item.setOrderId(orderId));
		bookOrderItemsRepository.saveAll(bookOrderItems);
		return orderId;
	}

	@Transactional(timeout = 5, rollbackFor = Exception.class)
	public void deleteBookOrder(Long id) {
		bookOrderItemsRepository.deleteByOrderId(id);
		bookOrdersRepository.deleteById(id);
	}

	public List<BookOrderVO> getBookOrderById(Long id) {
		// 1. Get the book order.
		Optional<BookOrders> order = bookOrdersRepository.findById(id);
		if (order.isEmpty()) {
			return Collections.emptyList();
		}

		// 2. Join the book details.
		Map<Long, List<BookOrderDetailVO>> detailsById = getBookDetails(List.of(id));

		// 3. Append the details to the response.
		return List.of(bookOrdersAndDetails2BookOrderVO(order.get(), detailsById));
	}

	public Page<BookOrderVO> queryBookOrdersByCriteria(BookOrdersSearchVO vo, Pageable pageable) {
		Specification<BookOrders> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (vo.getOrderId() != null) {
				predicates.add(builder.equal(root.get("orderId"), vo.getOrderId()));
			}
			if (vo.getUserId() != null) {
				predicates.add(builder.like(root.get("userId"), "%" + vo.getUserId() + "%"));
			}
			if (vo.getBookId() != null) {
				// BookOrders has no bookId attribute (entities are decoupled); match orders
				// that have at least one line item referencing the requested book.
				Subquery<Long> itemSub = query.subquery(Long.class);
				Root<BookOrderItems> item = itemSub.from(BookOrderItems.class);
				itemSub.select(item.get("orderId"));
				itemSub.where(builder.equal(item.get("orderId"), root.get("orderId")),
						builder.equal(item.join("bookId").get("bookId"), vo.getBookId()));
				predicates.add(builder.exists(itemSub));
			}
			if (vo.getOrderType() != null) {
				predicates.add(builder.like(root.get("orderType"), "%" + vo.getOrderType() + "%"));
			}
			if (vo.getStartDate() != null && vo.getEndDate() != null) {
				predicates.add(builder.between(root.<Date>get("createdAt"), vo.getStartDate(), vo.getEndDate()));
				predicates.add(builder.between(root.<Date>get("updatedAt"), vo.getStartDate(), vo.getEndDate()));
			}
			return builder.and(predicates.toArray(new Predicate[0]));
		};
		// 1. Get the requested page of book orders.
		Page<BookOrders> page = bookOrdersRepository.findAll(specification, pageable);

		// 2. Join the book details in one batch for this page.
		List<Long> orderIds = page.getContent().stream().map(BookOrders::getOrderId).toList();
		Map<Long, List<BookOrderDetailVO>> detailsById = getBookDetails(orderIds);

		// 3. Append the details to each response, preserving pagination metadata.
		return page.map(order -> bookOrdersAndDetails2BookOrderVO(order, detailsById));
	}

	/**
	 * Fetch details once; missing items produce an empty detail list during
	 * assembly.
	 */
	private Map<Long, List<BookOrderDetailVO>> getBookDetails(List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, List<BookOrderDetailVO>> detailsById = new HashMap<>();
		List<BookOrderItems> items = bookOrderItemsRepository.findByOrderIds(orderIds);
		for (BookOrderItems item : items) {
			detailsById.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>())
					.add(bookOrderItems2BookOrderDetailVO(item));
		}
		return detailsById;
	}

	/**
	 * Pure response assembly: no database calls or mutation of persisted entities.
	 */
	private BookOrderVO bookOrdersAndDetails2BookOrderVO(BookOrders source,
			Map<Long, List<BookOrderDetailVO>> detailsById) {
		BookOrderVO vo = new BookOrderVO();
		vo.setOrderId(source.getOrderId());
		vo.setUserId(source.getUserId());
		vo.setOrderType(source.getOrderType());
		vo.setTotalAmount(source.getTotalAmount());
		vo.setBookOrderDetailVOs(detailsById.getOrDefault(source.getOrderId(), Collections.emptyList()));
		return vo;
	}

	private BookOrderDetailVO bookOrderItems2BookOrderDetailVO(BookOrderItems item) {
		BookOrderDetailVO detail = new BookOrderDetailVO();
		detail.setBookId(item.getBookId().getBookId());
		detail.setQuantity(item.getQuantity());
		return detail;
	}

}
