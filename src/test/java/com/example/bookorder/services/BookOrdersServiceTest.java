package com.example.bookorder.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.example.bookorder.repository.BookOrderItemsRepository;
import com.example.bookorder.repository.BookOrdersRepository;
import com.example.bookorder.repository.BooksRepository;
import com.example.bookorder.repository.entity.BookOrderItems;
import com.example.bookorder.repository.entity.BookOrders;
import com.example.bookorder.repository.entity.Books;
import com.example.bookorder.services.vo.BookOrderVO;
import com.example.bookorder.services.vo.BookOrdersSearchVO;

@ExtendWith(MockitoExtension.class)
class BookOrdersServiceTest {
    @Mock
    private BookOrdersRepository orders;
    @Mock
    private BookOrderItemsRepository items;
    @Mock
    private BooksRepository books;
    @InjectMocks
    private BookOrdersService service;

    @Test
    void getOrderThenDetailsAndAppendToResponse() {
        BookOrders order = order(42L);
        when(orders.findById(42L)).thenReturn(Optional.of(order));
        when(items.findByOrderIds(List.of(42L))).thenReturn(List.of(item(42L, 8L, 2)));

        BookOrderVO result = service.getBookOrderById(42L).get(0);

        assertEquals(42L, result.getOrderId());
        assertEquals("user-1", result.getUserId());
        assertEquals("PURCHASE", result.getOrderType());
        assertEquals(new BigDecimal("19.90"), result.getTotalAmount());
        assertEquals(1, result.getBookOrderDetailVOs().size());
        assertEquals(8L, result.getBookOrderDetailVOs().get(0).getBookId());
        assertEquals(2, result.getBookOrderDetailVOs().get(0).getQuantity());
        InOrder sequence = inOrder(orders, items);
        sequence.verify(orders).findById(42L);
        sequence.verify(items).findByOrderIds(List.of(42L));
        verifyNoInteractions(books);
    }

    @Test
    void missingOrderSkipsDetailsQuery() {
        when(orders.findById(42L)).thenReturn(Optional.empty());

        assertTrue(service.getBookOrderById(42L).isEmpty());

        verifyNoInteractions(items, books);
    }

    @Test
    void queryGroupsDetailsByOrderAndPreservesPageMetadata() {
        PageRequest pageable = PageRequest.of(1, 3);
        Page<BookOrders> page = new PageImpl<>(List.of(order(41L), order(42L), order(43L)), pageable, 9);
        when(orders.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(items.findByOrderIds(List.of(41L, 42L, 43L))).thenReturn(List.of(
                item(42L, 8L, 2), item(41L, 9L, 1), item(42L, 10L, 3)));

        Page<BookOrderVO> result = service.queryBookOrdersByCriteria(new BookOrdersSearchVO(), pageable);

        assertEquals(9, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(3, result.getSize());
        assertEquals(9L, result.getContent().get(0).getBookOrderDetailVOs().get(0).getBookId());
        assertEquals(2, result.getContent().get(1).getBookOrderDetailVOs().size());
        assertEquals(8L, result.getContent().get(1).getBookOrderDetailVOs().get(0).getBookId());
        assertEquals(10L, result.getContent().get(1).getBookOrderDetailVOs().get(1).getBookId());
        assertTrue(result.getContent().get(2).getBookOrderDetailVOs().isEmpty());
        verify(items).findByOrderIds(List.of(41L, 42L, 43L));
        verifyNoMoreInteractions(items);
    }

    @Test
    void emptyPageSkipsDetailsQuery() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(orders.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        assertTrue(service.queryBookOrdersByCriteria(new BookOrdersSearchVO(), pageable).isEmpty());

        verifyNoInteractions(items, books);
    }

    @Test
    void detailsFailurePropagatesWithoutRetryOrPartialResponse() {
        when(orders.findById(42L)).thenReturn(Optional.of(order(42L)));
        IllegalStateException failure = new IllegalStateException("Details unavailable");
        when(items.findByOrderIds(List.of(42L))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getBookOrderById(42L)));

        verify(items).findByOrderIds(List.of(42L));
        verifyNoMoreInteractions(items);
    }

    private BookOrders order(Long id) {
        BookOrders order = new BookOrders();
        order.setOrderId(id);
        order.setUserId("user-1");
        order.setOrderType("PURCHASE");
        order.setTotalAmount(new BigDecimal("19.90"));
        return order;
    }

    private BookOrderItems item(Long orderId, Long bookId, int quantity) {
        Books book = new Books();
        book.setBookId(bookId);
        BookOrderItems item = new BookOrderItems();
        item.setOrderId(orderId);
        item.setBookId(book);
        item.setQuantity(quantity);
        return item;
    }
}
