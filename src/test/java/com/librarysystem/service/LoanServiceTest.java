package com.librarysystem.service;

import com.librarysystem.dto.response.LoanResponse;
import com.librarysystem.entity.Book;
import com.librarysystem.entity.Borrower;
import com.librarysystem.entity.Loan;
import com.librarysystem.entity.enums.BookStatus;
import com.librarysystem.entity.enums.LoanStatus;
import com.librarysystem.exception.BusinessException;
import com.librarysystem.exception.DuplicateActionException;
import com.librarysystem.exception.ResourceNotFoundException;
import com.librarysystem.repository.BookRepository;
import com.librarysystem.repository.BorrowerRepository;
import com.librarysystem.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, bookRepository, borrowerRepository);
    }

    @Test
    void borrowBookSuccessful() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findByIdForUpdate(bookId)).thenReturn(Optional.of(book));

        Loan saved = Loan.builder()
                .id(1L)
                .book(book)
                .borrower(borrower)
                .status(LoanStatus.ACTIVE)
                .borrowedAt(LocalDateTime.now())
                .build();
        when(loanRepository.save(any(Loan.class))).thenReturn(saved);

        // When
        LoanResponse response = loanService.borrowBook(borrowerId, bookId);

        // Then
        verify(borrowerRepository).findById(borrowerId);
        verify(bookRepository).findByIdForUpdate(bookId);
        verify(loanRepository).save(any(Loan.class));
        verify(bookRepository).save(any(Book.class));

        // Check loan that was saved
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan toSave = loanCaptor.getValue();
        assertThat(toSave.getBook()).isEqualTo(book);
        assertThat(toSave.getBorrower()).isEqualTo(borrower);
        assertThat(toSave.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(toSave.getBorrowedAt()).isNotNull();

        // Check book status updated
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        Book updatedBook = bookCaptor.getValue();
        assertThat(updatedBook.getStatus()).isEqualTo(BookStatus.BORROWED);

        // Check response mapping
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBookId()).isEqualTo(bookId);
        assertThat(response.getBookIsbn()).isEqualTo("978-1");
        assertThat(response.getBorrowerId()).isEqualTo(borrowerId);
        assertThat(response.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.getBorrowedAt()).isNotNull();
    }

    @Test
    void borrowBookWillThrowResourceNotFoundWhenBorrowerDoesNotExist() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        // When
        // Then
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.borrowBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Borrower not found with id: " + borrowerId);
        verify(borrowerRepository).findById(borrowerId);
        verifyNoInteractions(bookRepository, loanRepository);
    }

    @Test
    void borrowBookWillThrowResourceNotFoundWhenBookDoesNotExist() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findByIdForUpdate(bookId)).thenReturn(Optional.empty());

        // When
        // Then
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.borrowBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Book not found with id: " + bookId);
        verify(bookRepository).findByIdForUpdate(bookId);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBookWillThrowDuplicateActionWhenBookAlreadyBorrowed() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.BORROWED)
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findByIdForUpdate(bookId)).thenReturn(Optional.of(book));

        // When
        // Then
        DuplicateActionException ex = assertThrows(
                DuplicateActionException.class,
                () -> loanService.borrowBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Book is already borrowed");
        verify(loanRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void returnBookSuccessful() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.BORROWED)
                .build();

        Loan activeLoan = Loan.builder()
                .id(1L)
                .book(book)
                .borrower(borrower)
                .status(LoanStatus.ACTIVE)
                .borrowedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.findByBookAndStatus(book, LoanStatus.ACTIVE)).thenReturn(Optional.of(activeLoan));

        Loan updatedLoan = Loan.builder()
                .id(1L)
                .book(book)
                .borrower(borrower)
                .status(LoanStatus.RETURNED)
                .borrowedAt(activeLoan.getBorrowedAt())
                .returnedAt(LocalDateTime.now())
                .build();
        when(loanRepository.save(any(Loan.class))).thenReturn(updatedLoan);

        // When
        LoanResponse response = loanService.returnBook(borrowerId, bookId);

        // Then
        verify(borrowerRepository).findById(borrowerId);
        verify(bookRepository).findById(bookId);
        verify(loanRepository).findByBookAndStatus(book, LoanStatus.ACTIVE);
        verify(loanRepository).save(any(Loan.class));
        verify(bookRepository).save(any(Book.class));

        // Loan saved should be marked RETURNED
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan savedLoan = loanCaptor.getValue();
        assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(savedLoan.getReturnedAt()).isNotNull();

        // Book should be AVAILABLE
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        Book updatedBook = bookCaptor.getValue();
        assertThat(updatedBook.getStatus()).isEqualTo(BookStatus.AVAILABLE);

        // Response mapping
        assertThat(response.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.getReturnedAt()).isNotNull();
    }

    @Test
    void returnBookWillThrowResourceNotFoundWhenBorrowerDoesNotExist() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        // When
        // Then
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.returnBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Borrower not found with id: " + borrowerId);
        verifyNoInteractions(bookRepository, loanRepository);
    }

    @Test
    void returnBookWillThrowResourceNotFoundWhenBookDoesNotExist() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        // When
        // Then
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.returnBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Book not found with id: " + bookId);
        verify(loanRepository, never()).findByBookAndStatus(any(), any());
    }

    @Test
    void returnBookWillThrowBusinessExceptionWhenBookIsAlreadyAvailable() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.AVAILABLE)
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        // When
        // Then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> loanService.returnBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Book is not currently borrowed");
        verify(loanRepository, never()).findByBookAndStatus(any(), any());
    }

    @Test
    void returnBookWillThrowBusinessExceptionWhenNoActiveLoanExists() {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.BORROWED)
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.findByBookAndStatus(book, LoanStatus.ACTIVE)).thenReturn(Optional.empty());

        // When
        // Then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> loanService.returnBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("Book is not currently borrowed");
        verify(loanRepository).findByBookAndStatus(book, LoanStatus.ACTIVE);
        verify(loanRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void returnBookWillThrowBusinessExceptionWhenBorrowerIsNotInLoan() {
        // Given
        Long borrowerId = 1L;
        Long otherBorrowerId = 2L;
        Long bookId = 1L;

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Borrower otherBorrower = Borrower.builder()
                .id(otherBorrowerId)
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        Book book = Book.builder()
                .id(bookId)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .status(BookStatus.BORROWED)
                .build();

        Loan activeLoan = Loan.builder()
                .id(1L)
                .book(book)
                .borrower(otherBorrower)
                .status(LoanStatus.ACTIVE)
                .borrowedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.findByBookAndStatus(book, LoanStatus.ACTIVE)).thenReturn(Optional.of(activeLoan));

        // When
        // Then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> loanService.returnBook(borrowerId, bookId)
        );

        assertThat(ex.getMessage()).isEqualTo("This book is currently borrowed by a different person");
        verify(loanRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
    }

}