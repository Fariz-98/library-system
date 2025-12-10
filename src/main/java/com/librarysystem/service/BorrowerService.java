package com.librarysystem.service;

import com.librarysystem.dto.request.CreateBorrowerRequest;
import com.librarysystem.dto.response.BorrowerResponse;
import com.librarysystem.entity.Borrower;
import com.librarysystem.exception.DuplicateActionException;
import com.librarysystem.repository.BorrowerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;

    public BorrowerService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional
    public BorrowerResponse createBorrower(CreateBorrowerRequest request) {
        log.info("Registering borrower with email={}", request.getEmail());

        if (borrowerRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to register borrower with existing email={}", request.getEmail());
            throw new DuplicateActionException("Email is taken");
        }

        Borrower borrower = Borrower.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();

        Borrower saved = borrowerRepository.save(borrower);

        return BorrowerResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .build();
    }
}
