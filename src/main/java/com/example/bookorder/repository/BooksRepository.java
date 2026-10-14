package com.example.bookorder.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.example.bookorder.repository.entity.Books;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface BooksRepository extends JpaRepository<Books, Long>, JpaSpecificationExecutor<Books> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
	@Query("select b from Books b where b.bookId = :bookId")
	Optional<Books> lockByBookId(@Param("bookId") Long bookId);
}