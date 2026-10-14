package com.example.bookorder.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bookorder.repository.BooksRepository;
import com.example.bookorder.repository.entity.Books;
import com.example.bookorder.services.vo.BookVO;

/**
 * Simple happy-path unit tests for {@link BooksService}.
 *
 * <p>
 * The repository is mocked, so these tests verify the service's own behaviour:
 * copying VO to entity, delegating persistence calls, and returning the
 * generated id. No database or Spring context is required.
 */
@ExtendWith(MockitoExtension.class)
class BooksServiceTest {

	@Mock
	private BooksRepository booksRepository;

	@InjectMocks
	private BooksService booksService;

	@Test
	void savePersistsBookAndReturnsGeneratedId() {
		BookVO request = new BookVO();
		request.setTitle("Clean Code");
		request.setAuthor("Robert Martin");
		request.setPrice(BigDecimal.valueOf(29.99));

		given(booksRepository.save(any(Books.class))).willAnswer(invocation -> {
			Books saved = invocation.getArgument(0);
			saved.setBookId(101L);
			return saved;
		});

		Long id = booksService.save(request);

		assertEquals(101L, id);
		ArgumentCaptor<Books> captor = ArgumentCaptor.forClass(Books.class);
		verify(booksRepository).save(captor.capture());
		Books toPersist = captor.getValue();
		assertEquals("Clean Code", toPersist.getTitle());
		assertEquals("Robert Martin", toPersist.getAuthor());
		assertEquals(BigDecimal.valueOf(29.99), toPersist.getPrice());
	}

	@Test
	void batchSaveReturnsGeneratedIdsInOrder() {
		List<BookVO> requests = List.of(bookVo("Book A"), bookVo("Book B"));

		given(booksRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<Books> books = invocation.getArgument(0);
			for (int i = 0; i < books.size(); i++) {
				books.get(i).setBookId((long) (i + 1));
			}
			return books;
		});

		List<Long> ids = booksService.batchSave(requests);

		assertEquals(List.of(1L, 2L), ids);
		verify(booksRepository).saveAll(anyList());
	}

	@Test
	void deleteDelegatesToRepository() {
		booksService.delete(5L);

		verify(booksRepository).deleteById(5L);
		verifyNoMoreInteractions(booksRepository);
	}

	@Test
	void getByIdMapsEntityBackToVo() {
		Books entity = new Books();
		entity.setBookId(9L);
		entity.setTitle("Refactoring");
		entity.setAuthor("Martin Fowler");
		entity.setPrice(BigDecimal.valueOf(45.00));
		when(booksRepository.findById(9L)).thenReturn(java.util.Optional.of(entity));

		List<BookVO> result = booksService.queryById(9L);

		assertEquals(1, result.size());
		assertEquals(9L, result.get(0).getBookId());
		assertEquals("Refactoring", result.get(0).getTitle());
		assertEquals("Martin Fowler", result.get(0).getAuthor());
	}

	private BookVO bookVo(String title) {
		BookVO vo = new BookVO();
		vo.setTitle(title);
		vo.setAuthor("Author");
		vo.setPrice(BigDecimal.ONE);
		return vo;
	}
}