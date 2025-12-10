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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BorrowerRepository borrowerRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, BorrowerRepository borrowerRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional
    public LoanResponse borrowBook(Long borrowerId, Long bookId) {
        log.info("Borrow request: borrowerId={}, bookId={}", borrowerId, bookId);

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        // Check availability
        if (book.getStatus() == BookStatus.BORROWED) {
            log.warn("Book id={} is already on an active loan", book.getId());
            throw new DuplicateActionException("Book is already borrowed");
        }

        Loan loan = Loan.builder()
                .book(book)
                .borrower(borrower)
                .status(LoanStatus.ACTIVE)
                .borrowedAt(LocalDateTime.now())
                .build();
        Loan saved = loanRepository.save(loan);

        book.setStatus(BookStatus.BORROWED);
        bookRepository.save(book);

        return toLoanResponse(saved);
    }

    @Transactional
    public LoanResponse returnBook(Long borrowerId, Long bookId) {
        log.info("Return request: borrowerId={}, bookId={}", borrowerId, bookId);

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (book.getStatus() == BookStatus.AVAILABLE) {
            log.warn("Attempt to return bookId={} which is already AVAILABLE", bookId);
            throw new BusinessException("Book is not currently borrowed");
        }

        Loan activeLoan = loanRepository.findByBookAndStatus(book, LoanStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("Attempt to return a book that is not currently on active loan, bookId={}", bookId);
                    return new BusinessException("Book is not currently borrowed");
                });

        if (!activeLoan.getBorrower().getId().equals(borrower.getId())) {
            log.warn("Borrower id={} attempted to return book id={} currently borrowed by id={}",
                    borrowerId, bookId, activeLoan.getBorrower().getId());
            throw new BusinessException("This book is currently borrowed by a different person");
        }

        activeLoan.setStatus(LoanStatus.RETURNED);
        activeLoan.setReturnedAt(LocalDateTime.now());
        Loan updated = loanRepository.save(activeLoan);

        book.setStatus(BookStatus.AVAILABLE);
        bookRepository.save(book);

        return toLoanResponse(updated);
    }

    private LoanResponse toLoanResponse(Loan loan) {
        Book book = loan.getBook();
        Borrower borrower = loan.getBorrower();

        return LoanResponse.builder()
                .id(loan.getId())
                .bookId(book.getId())
                .bookIsbn(book.getIsbn())
                .bookTitle(book.getTitle())
                .bookAuthor(book.getAuthor())
                .borrowerId(borrower.getId())
                .borrowerName(borrower.getName())
                .borrowerEmail(borrower.getEmail())
                .status(loan.getStatus())
                .borrowedAt(loan.getBorrowedAt())
                .returnedAt(loan.getReturnedAt())
                .build();
    }
}
