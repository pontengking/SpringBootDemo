package com.example.bookorder.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.bookorder.controller.converter.BookOrderConverter;
import com.example.bookorder.controller.dto.request.BookOrderSearchRequest;
import com.example.bookorder.controller.dto.response.BaseResponse;
import com.example.bookorder.controller.dto.response.BookOrderResponse;
import com.example.bookorder.services.BookOrdersService;
import com.example.bookorder.services.vo.BookOrderVO;
import com.example.bookorder.services.vo.BookOrdersSearchVO;

/** Unit tests only: Spring validation, security and transactions are not started. */
@ExtendWith(MockitoExtension.class)
class BookOrdersControllerTest {

    @Mock
    private BookOrdersService service;

    @Mock
    private BookOrderConverter converter;

    @InjectMocks
    private BookOrdersController controller;

    @AfterEach
    void clearTraceId() {
        MDC.remove("traceId");
    }

    @Test
    void saveReturnsCreatedIdAsString() {
        BookOrderVO request = new BookOrderVO();
        request.setBookOrderDetailVOs(List.of());
        when(service.saveBookOrder(request)).thenReturn(42L);

        assertEquals("42", controller.save(request));

        verify(service).saveBookOrder(request);
    }

    @Test
    void savePropagatesServiceFailureWithoutRetry() {
        BookOrderVO request = new BookOrderVO();
        IllegalStateException failure = new IllegalStateException("Save failed");
        when(service.saveBookOrder(request)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> controller.save(request)));

        verify(service).saveBookOrder(request);
        verifyNoMoreInteractions(service);
    }

    @Test
    void deleteDelegatesToService() {
        controller.delete(42L);

        verify(service).deleteBookOrder(42L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void deletePropagatesServiceFailureWithoutRetry() {
        IllegalStateException failure = new IllegalStateException("Delete failed");
        doThrow(failure).when(service).deleteBookOrder(42L);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> controller.delete(42L)));

        verify(service).deleteBookOrder(42L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void getByIdReturnsConvertedDataAndTraceId() {
        BookOrderVO order = new BookOrderVO();
        BookOrderResponse expected = new BookOrderResponse(
                42L, "7", "PURCHASE", List.of(), new BigDecimal("19.90"));
        when(service.getBookOrderById(42L)).thenReturn(List.of(order));
        when(converter.convert2Response(order)).thenReturn(expected);
        MDC.put("traceId", "test-trace-id");

        BaseResponse<List<BookOrderResponse>> result = controller.getById(42L);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(List.of(expected), result.getData());
        assertEquals("test-trace-id", result.getTraceId());
        assertNull(result.getErrorCode());
        assertNull(result.getErrorDesc());
        verify(service).getBookOrderById(42L);
        verify(converter).convert2Response(order);
    }

    @Test
    void getByIdReturnsEmptyDataWhenOrderIsMissing() {
        when(service.getBookOrderById(42L)).thenReturn(List.of());
        MDC.remove("traceId");

        BaseResponse<List<BookOrderResponse>> result = controller.getById(42L);

        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getData().isEmpty());
        assertNull(result.getTraceId());
        verifyNoInteractions(converter);
    }

    @Test
    void getByIdPropagatesServiceFailure() {
        IllegalStateException failure = new IllegalStateException("Read failed");
        when(service.getBookOrderById(42L)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> controller.getById(42L)));

        verifyNoInteractions(converter);
    }

    @Test
    void queryPassesConvertedCriteriaAndPaginationToService() {
        BookOrderSearchRequest request = new BookOrderSearchRequest();
        request.setPage(2);
        request.setPageSize(10);
        BookOrdersSearchVO criteria = new BookOrdersSearchVO();
        PageRequest pageable = PageRequest.of(2, 10);
        BookOrderVO order = new BookOrderVO();
        BookOrderResponse response = new BookOrderResponse(42L, "7", "PURCHASE", List.of(), BigDecimal.TEN);
        Page<BookOrderVO> expected = new PageImpl<>(List.of(order), pageable, 21);
        when(converter.convert2Response(order)).thenReturn(response);
        when(converter.convert2SearchBookOrderVO(request)).thenReturn(criteria);
        when(service.queryBookOrdersByCriteria(criteria, pageable)).thenReturn(expected);

        Page<BookOrderResponse> result = controller.query(request);
        assertEquals(List.of(response), result.getContent());
        assertEquals(21, result.getTotalElements());
        assertEquals(pageable, result.getPageable());

        verify(converter).convert2SearchBookOrderVO(request);
        verify(service).queryBookOrdersByCriteria(criteria, pageable);
    }

    @Test
    void queryUsesDefaultPaginationAndReturnsEmptyPage() {
        BookOrderSearchRequest request = new BookOrderSearchRequest();
        BookOrdersSearchVO criteria = new BookOrdersSearchVO();
        PageRequest pageable = PageRequest.of(0, 20);
        Page<BookOrderVO> expected = Page.empty(pageable);
        when(converter.convert2SearchBookOrderVO(request)).thenReturn(criteria);
        when(service.queryBookOrdersByCriteria(criteria, pageable)).thenReturn(expected);

        Page<BookOrderResponse> result = controller.query(request);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(pageable, result.getPageable());

        verify(service).queryBookOrdersByCriteria(criteria, pageable);
    }

    @Test
    void queryRejectsNegativePageBeforeCallingDependencies() {
        BookOrderSearchRequest request = new BookOrderSearchRequest();
        request.setPage(-1);

        assertThrows(IllegalArgumentException.class, () -> controller.query(request));

        verifyNoInteractions(service, converter);
    }

    @Test
    void queryRejectsZeroPageSizeBeforeCallingDependencies() {
        BookOrderSearchRequest request = new BookOrderSearchRequest();
        request.setPageSize(0);

        assertThrows(IllegalArgumentException.class, () -> controller.query(request));

        verifyNoInteractions(service, converter);
    }

    @Test
    void queryPropagatesServiceFailure() {
        BookOrderSearchRequest request = new BookOrderSearchRequest();
        BookOrdersSearchVO criteria = new BookOrdersSearchVO();
        PageRequest pageable = PageRequest.of(0, 20);
        IllegalStateException failure = new IllegalStateException("Query failed");
        when(converter.convert2SearchBookOrderVO(request)).thenReturn(criteria);
        when(service.queryBookOrdersByCriteria(criteria, pageable)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> controller.query(request)));
    }
}
