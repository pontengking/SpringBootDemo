package com.example.bookorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.example.bookorder.repository.entity.BookOrderItems;

public interface BookOrderItemsRepository
		extends
			JpaRepository<BookOrderItems, Long>,
			JpaSpecificationExecutor<BookOrderItems> {

	@Query("delete from BookOrderItems b where b.orderId = :orderId")
	void deleteByOrderId(Long orderId);

}