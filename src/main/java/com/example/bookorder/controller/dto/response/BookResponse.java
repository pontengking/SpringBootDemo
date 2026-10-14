package com.example.bookorder.controller.dto.response;

import java.math.BigDecimal;

/**
 * HTTP response contract.
 */
public record BookResponse(Long id, String title, String author, BigDecimal price, int unitsInStock) {
}
