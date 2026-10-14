package com.example.bookorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.bookorder.repository.entity.BookOrders;

public interface BookOrdersRepository extends JpaRepository<BookOrders, Long>, JpaSpecificationExecutor<BookOrders> {
}