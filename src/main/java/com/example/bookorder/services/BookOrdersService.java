package com.example.bookorder.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookorder.repository.BookOrderItemsRepository;
import com.example.bookorder.repository.BookOrdersRepository;
import com.example.bookorder.repository.entity.BookOrderItems;
import com.example.bookorder.repository.entity.BookOrders;
import com.example.bookorder.services.vo.BookOrderVO;
import com.example.bookorder.services.vo.BookOrdersSearchVO;

import jakarta.persistence.criteria.Predicate;

@Service
public class BookOrdersService {

	private BookOrdersRepository bookOrdersRepository;
	private BookOrderItemsRepository bookOrderItemsRepository;

	@Transactional(timeout = 5, rollbackFor = Exception.class)
	public Long saveBookOrder(List<BookOrderVO> vO) {
		BookOrders order = new BookOrders();
		BeanUtils.copyProperties(vO, order);
		BookOrders bookOrder = bookOrdersRepository.save(order);

		List<BookOrderItems> bookOrderItems = vO.stream().map(vo -> {
			BookOrderItems bookOrderItem = new BookOrderItems();
			BeanUtils.copyProperties(vo, bookOrderItem);
			bookOrderItem.setBookOrder(bookOrder);
			return bookOrderItem;
		}).toList();
		bookOrderItemsRepository.saveAll(bookOrderItems);
		return bookOrder.getOrderId();
	}

	@Transactional(timeout = 5, rollbackFor = Exception.class)
	public void deleteBookOrder(Long id) {
		bookOrderItemsRepository.deleteByOrderId(id);
		bookOrdersRepository.deleteById(id);
	}

	public List<BookOrderVO> getBookOrderById(Long id) {
		return bookOrdersRepository.findById(id).map(bookOrder -> Collections.singletonList(toVO(bookOrder)))
				.orElseGet(Collections::emptyList);
	}

	public Page<BookOrderVO> queryBookOrdersByCriteria(BookOrdersSearchVO vo, Pageable pageable) {
		Specification<BookOrders> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (vo.getOrderId() != null) {
				predicates.add(builder.equal(root.get("orderId"), vo.getOrderId()));
			}
			if (vo.getUserId() != null) {
				predicates.add(builder.equal(root.get("userId"), vo.getUserId()));
			}
			if (vo.getBookId() != null) {
				predicates.add(builder.equal(root.get("bookId"), vo.getBookId()));
			}
			if (vo.getOrderType() != null) {
				predicates.add(builder.equal(root.get("orderType"), vo.getOrderType()));
			}
			if (vo.getStartDate() != null && vo.getEndDate() != null) {
				predicates.add(builder.between(root.<Date>get("createdAt"), vo.getStartDate(), vo.getEndDate()));
				predicates.add(builder.between(root.<Date>get("updatedAt"), vo.getStartDate(), vo.getEndDate()));
			}
			return builder.and(predicates.toArray(new Predicate[0]));
		};
		return bookOrdersRepository.findAll(specification, pageable).map(this::toVO);

	}

	private BookOrderVO toVO(BookOrders source) {
		BookOrderVO vo = new BookOrderVO();
		BeanUtils.copyProperties(source, vo);
		return vo;
	}

}
