package com.example.bookorder.repository.entity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
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
@Table(name = "book_order_items")
public class BookOrderItems implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "order_item_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderItemId;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "book_id", referencedColumnName = "book_id", nullable = false)
	private Books bookId;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "item_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal itemTotal;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Date createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Date updatedAt;

}
