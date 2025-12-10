package com.librarysystem.service;

import com.librarysystem.dto.request.CreateBookRequest;
import com.librarysystem.dto.response.BookResponse;
import com.librarysystem.entity.Book;
import com.librarysystem.exception.BusinessException;
import com.librarysystem.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        log.info("Registering book with isbn={}", request.getIsbn());

        Optional<Book> existingOptional = bookRepository.findFirstByIsbn(request.getIsbn());
        if (existingOptional.isPresent()) {
            Book existing = existingOptional.get();

            boolean titleMismatch = !existing.getTitle().equals(request.getTitle());
            boolean authorMismatch = !existing.getAuthor().equals(request.getAuthor());

            if (titleMismatch || authorMismatch) {
                log.warn("ISBN conflict for isbn={}, existing book title={}, author={}. new book title={}, author={}",
                        request.getIsbn(),
                        existing.getTitle(), existing.getAuthor(),
                        request.getTitle(), request.getAuthor());

                throw new BusinessException("ISBN already exists with different title/author");
            }
        }

        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .build();

        Book saved = bookRepository.save(book);

        return toBookResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> getBooks(Pageable pageable) {
        log.info("Fetching all books");
        return bookRepository.findAll(pageable)
                .map(book -> toBookResponse(book));
    }

    private BookResponse toBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .bookStatus(book.getStatus())
                .build();
    }

}
