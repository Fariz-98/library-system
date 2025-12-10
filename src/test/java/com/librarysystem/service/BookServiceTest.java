package com.librarysystem.service;

import com.librarysystem.dto.request.CreateBookRequest;
import com.librarysystem.dto.response.BookResponse;
import com.librarysystem.entity.Book;
import com.librarysystem.entity.enums.BookStatus;
import com.librarysystem.exception.BusinessException;
import com.librarysystem.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository);
    }

    @Test
    void createBookSuccessful() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .build();

        when(bookRepository.findFirstByIsbn("978-1")).thenReturn(Optional.empty());

        Book saved = Book.builder()
                .id(1L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        // When
        BookResponse response = bookService.createBook(request);

        // Then
        verify(bookRepository).findFirstByIsbn("978-1");
        verify(bookRepository).save(any(Book.class));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book toSave = captor.getValue();

        assertThat(toSave.getId()).isNull();
        assertThat(toSave.getIsbn()).isEqualTo("978-1");
        assertThat(toSave.getTitle()).isEqualTo("Clean Code");
        assertThat(toSave.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(toSave.getStatus()).isEqualTo(BookStatus.AVAILABLE);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getIsbn()).isEqualTo("978-1");
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(response.getBookStatus()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void createBookSuccessfulWhenExistingCopyHasSameMetadata() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .build();

        Book existing = Book.builder()
                .id(1L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();

        when(bookRepository.findFirstByIsbn("978-1")).thenReturn(Optional.of(existing));

        Book saved = Book.builder()
                .id(2L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        // When
        BookResponse response = bookService.createBook(request);

        // Then
        verify(bookRepository).findFirstByIsbn("978-1");
        verify(bookRepository).save(any(Book.class));

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getIsbn()).isEqualTo("978-1");
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getAuthor()).isEqualTo("Robert C. Martin");
    }

    @Test
    void createBookWillThrowExceptionWhenIsbnConflict() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
                .isbn("978-1")
                .title("Some Other Title")
                .author("Someone Else")
                .build();

        Book existing = Book.builder()
                .id(1L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();

        when(bookRepository.findFirstByIsbn("978-1")).thenReturn(Optional.of(existing));

        // When
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> bookService.createBook(request)
        );

        // Then
        assertThat(ex.getMessage()).isEqualTo("ISBN already exists with different title/author");
        verify(bookRepository).findFirstByIsbn("978-1");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void getBooksSuccessful() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        Book book1 = Book.builder()
                .id(1L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .isbn("978-2")
                .title("Effective Java")
                .author("Joshua Bloch")
                .status(BookStatus.BORROWED)
                .build();

        Page<Book> page = new PageImpl<>(List.of(book1, book2), pageable, 2);
        when(bookRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<BookResponse> result = bookService.getBooks(pageable);

        // Then
        verify(bookRepository).findAll(pageable);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        BookResponse first = result.getContent().get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getIsbn()).isEqualTo("978-1");
        assertThat(first.getBookStatus()).isEqualTo(BookStatus.AVAILABLE);

        BookResponse second = result.getContent().get(1);
        assertThat(second.getId()).isEqualTo(2L);
        assertThat(second.getIsbn()).isEqualTo("978-2");
        assertThat(second.getBookStatus()).isEqualTo(BookStatus.BORROWED);
    }

}