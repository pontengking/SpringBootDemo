package com.example.bookorder.repository.entity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
@Table(name = "books")
public class Books implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "book_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bookId;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "author", nullable = false, length = 150)
	private String author;

	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@NotNull
	@PositiveOrZero
	@Column(name = "units_in_stock", nullable = false)
	private Integer unitsInStock;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Date createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Date updatedAt;
}
