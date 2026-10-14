package com.example.bookorder.controller.dto.response;

import java.util.List;

/**
 * HTTP response contract.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
