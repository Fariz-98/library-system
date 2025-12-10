package com.librarysystem.controller;

import com.librarysystem.dto.response.LoanResponse;
import com.librarysystem.entity.enums.LoanStatus;
import com.librarysystem.exception.BusinessException;
import com.librarysystem.exception.DuplicateActionException;
import com.librarysystem.exception.GlobalExceptionHandler;
import com.librarysystem.exception.ResourceNotFoundException;
import com.librarysystem.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoanController.class)
@Import(GlobalExceptionHandler.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void borrowBookSuccessful() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        LoanResponse response = LoanResponse.builder()
                .id(1L)
                .bookId(bookId)
                .bookIsbn("978-1")
                .bookTitle("Clean Code")
                .bookAuthor("Robert C. Martin")
                .borrowerId(borrowerId)
                .borrowerName("John Doe")
                .borrowerEmail("john@example.com")
                .status(LoanStatus.ACTIVE)
                .borrowedAt(LocalDateTime.now())
                .build();

        given(loanService.borrowBook(borrowerId, bookId)).willReturn(response);

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/borrow/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.bookId").value(1L))
                .andExpect(jsonPath("$.borrowerId").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void borrowBookWillReturnNotFoundWhenBorrowerDoesNotExist() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        given(loanService.borrowBook(borrowerId, bookId))
                .willThrow(new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/borrow/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Borrower not found with id: " + borrowerId))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/borrow/1"));
    }

    @Test
    void borrowBookWillReturnNotFoundWhenBookDoesNotExist() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 999L;

        given(loanService.borrowBook(borrowerId, bookId))
                .willThrow(new ResourceNotFoundException("Book not found with id: " + bookId));

        // When
        // THen
        mockMvc.perform(post("/api/borrowers/{borrowerId}/borrow/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Book not found with id: " + bookId))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/borrow/999"));
    }

    @Test
    void borrowBookWillReturnConflictWhenBookAlreadyBorrowed() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        given(loanService.borrowBook(borrowerId, bookId))
                .willThrow(new DuplicateActionException("Book is already borrowed"));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/borrow/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Book is already borrowed"))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/borrow/1"));
    }

    @Test
    void returnBookSuccessful() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        LoanResponse response = LoanResponse.builder()
                .id(1L)
                .bookId(bookId)
                .bookIsbn("978-1")
                .bookTitle("Clean Code")
                .bookAuthor("Robert C. Martin")
                .borrowerId(borrowerId)
                .borrowerName("John Doe")
                .borrowerEmail("john@example.com")
                .status(LoanStatus.RETURNED)
                .borrowedAt(LocalDateTime.now().minusDays(1))
                .returnedAt(LocalDateTime.now())
                .build();

        given(loanService.returnBook(borrowerId, bookId)).willReturn(response);

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/return/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.bookId").value(1L))
                .andExpect(jsonPath("$.borrowerId").value(1L));
    }

    @Test
    void returnBookWillReturnNotFoundWhenBorrowerDoesNotExist() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        given(loanService.returnBook(borrowerId, bookId))
                .willThrow(new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/return/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Borrower not found with id: " + borrowerId))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/return/1"));
    }

    @Test
    void returnBookWillReturnNotFoundWhenBookDoesNotExist() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        given(loanService.returnBook(borrowerId, bookId))
                .willThrow(new ResourceNotFoundException("Book not found with id: " + bookId));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/return/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Book not found with id: " + bookId))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/return/1"));
    }

    @Test
    void returnBookWillReturnBadRequestWhenBookIsNotCurrentlyBorrowed() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        given(loanService.returnBook(borrowerId, bookId))
                .willThrow(new BusinessException("Book is not currently borrowed"));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/return/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Book is not currently borrowed"))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/return/1"));
    }

    @Test
    void returnBookWillReturnBadRequestWhenBookIsBorrowedByDifferentPerson() throws Exception {
        // Given
        Long borrowerId = 1L;
        Long bookId = 1L;

        given(loanService.returnBook(borrowerId, bookId))
                .willThrow(new BusinessException("This book is currently borrowed by a different person"));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers/{borrowerId}/return/{bookId}", borrowerId, bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("This book is currently borrowed by a different person"))
                .andExpect(jsonPath("$.path").value("/api/borrowers/1/return/1"));
    }

}