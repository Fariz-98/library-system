package com.librarysystem.repository;

import com.librarysystem.entity.Book;
import com.librarysystem.entity.Loan;
import com.librarysystem.entity.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByBookAndStatus(Book book, LoanStatus status);

}
