package com.librarysystem.repository;

import com.librarysystem.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerRepository extends JpaRepository<Borrower, Long> {

    boolean existsByEmail(String email);

}
