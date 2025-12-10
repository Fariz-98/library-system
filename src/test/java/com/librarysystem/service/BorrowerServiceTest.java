package com.librarysystem.service;

import com.librarysystem.dto.request.CreateBorrowerRequest;
import com.librarysystem.dto.response.BorrowerResponse;
import com.librarysystem.entity.Borrower;
import com.librarysystem.exception.DuplicateActionException;
import com.librarysystem.repository.BorrowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    private BorrowerService borrowerService;

    @BeforeEach
    void setUp() {
        borrowerService = new BorrowerService(borrowerRepository);
    }

    @Test
    void createBorrowerSuccessful() {
        // Given
        CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();

        when(borrowerRepository.existsByEmail("john@example.com")).thenReturn(false);

        Borrower saved = Borrower.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();
        when(borrowerRepository.save(any(Borrower.class))).thenReturn(saved);

        // When
        BorrowerResponse response = borrowerService.createBorrower(request);

        // Then
        verify(borrowerRepository).existsByEmail("john@example.com");
        verify(borrowerRepository).save(any(Borrower.class));

        ArgumentCaptor<Borrower> captor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(captor.capture());
        Borrower toSave = captor.getValue();

        assertThat(toSave.getId()).isNull();
        assertThat(toSave.getName()).isEqualTo("John Doe");
        assertThat(toSave.getEmail()).isEqualTo("john@example.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void createBorrowerWillThrowExceptionWhenEmailIsTaken() {
        // Given
        CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();

        when (borrowerRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When
        // Then
        DuplicateActionException ex = assertThrows(DuplicateActionException.class,
                () -> borrowerService.createBorrower(request));
        assertThat(ex.getMessage()).isEqualTo("Email is taken");
        verify(borrowerRepository).existsByEmail("john@example.com");
        verify(borrowerRepository, never()).save(any(Borrower.class));
    }
}