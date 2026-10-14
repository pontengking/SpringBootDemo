package com.example.bookorder.controller.support;

import java.util.Objects;
import org.springframework.data.domain.Page;
import com.example.bookorder.controller.dto.response.PageResponse;

/** Shared HTTP pagination envelope mapping; no feature-specific fields. */
public final class PageResponseConverter {
    private PageResponseConverter() {
    }

    public static <T> PageResponse<T> page2PageResponse(Page<T> source) {
        Objects.requireNonNull(source, "source");
        return new PageResponse<>(source.getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
