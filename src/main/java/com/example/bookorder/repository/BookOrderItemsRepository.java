package com.example.bookorder.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bookorder.repository.entity.BookOrderItems;

public interface BookOrderItemsRepository
		extends
			JpaRepository<BookOrderItems, Long>,
			JpaSpecificationExecutor<BookOrderItems> {

	@Modifying
	@Query("delete from BookOrderItems b where b.orderId = :orderId")
	void deleteByOrderId(@Param("orderId") Long orderId);

	List<BookOrderItems> findByOrderId(Long orderId);

	@Query("select i from BookOrderItems i join i.bookId b where i.orderId in :orderIds")
	List<BookOrderItems> findByOrderIds(@Param("orderIds") Collection<Long> orderIds);

}
