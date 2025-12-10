package com.librarysystem.controller;

import com.librarysystem.dto.request.CreateBorrowerRequest;
import com.librarysystem.dto.response.BorrowerResponse;
import com.librarysystem.exception.DuplicateActionException;
import com.librarysystem.exception.GlobalExceptionHandler;
import com.librarysystem.service.BorrowerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BorrowerController.class)
@Import(GlobalExceptionHandler.class)
class BorrowerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowerService borrowerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createBorrowerSuccessful() throws Exception {
        // Given
        CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();

        BorrowerResponse response = BorrowerResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        given(borrowerService.createBorrower(any(CreateBorrowerRequest.class)))
                .willReturn(response);

        // When
        // Then
        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/borrowers/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createBorrowerWillReturnBadRequestWhenValidationFails() throws Exception {
        // Given
        CreateBorrowerRequest invalidRequest = CreateBorrowerRequest.builder()
                .name("")
                .email("not-email")
                .build();

        // When
        // Then
        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", notNullValue()))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void createBorrowerWillReturnConflictWhenEmailIsTaken() throws Exception {
        // Given
        CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();

        given(borrowerService.createBorrower(any(CreateBorrowerRequest.class)))
                .willThrow(new DuplicateActionException("Email is taken"));

        // When
        // Then
        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email is taken"))
                .andExpect(jsonPath("$.path").value("/api/borrowers"));
    }

}