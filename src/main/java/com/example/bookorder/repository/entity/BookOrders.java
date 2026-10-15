package com.example.bookorder.repository.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@Table(name = "book_orders")
public class BookOrders implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "order_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderId;

	@Column(name = "user_id", nullable = false)
	@Size(max = 32, message = "User ID should not be longer than 32 characters")
	private String userId;

	@Column(name = "order_type", nullable = false)
	@Size(max = 32, message = "Order type should not be longer than 32 characters")
	private String orderType;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Date createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Date updatedAt;

}
