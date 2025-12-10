package com.librarysystem.controller;

import com.librarysystem.dto.request.CreateBookRequest;
import com.librarysystem.dto.response.BookResponse;
import com.librarysystem.entity.enums.BookStatus;
import com.librarysystem.exception.BusinessException;
import com.librarysystem.exception.GlobalExceptionHandler;
import com.librarysystem.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookController.class)
@Import(GlobalExceptionHandler.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createBookSuccessful() throws Exception {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .build();

        BookResponse response = BookResponse.builder()
                .id(1L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .bookStatus(BookStatus.AVAILABLE)
                .build();

        given(bookService.createBook(any(CreateBookRequest.class)))
                .willReturn(response);

        // When
        // Then
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/books/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.isbn").value("978-1"))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.bookStatus").value("AVAILABLE"));
    }

    @Test
    void createBookWillReturnBadRequestWhenValidationFails() throws Exception {
        // Given
        CreateBookRequest invalidRequest = CreateBookRequest.builder()
                .isbn("")
                .title("")
                .author("")
                .build();

        // When
        // Then
        mockMvc.perform(post("/api/books")
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
    void createBookWillReturnBadRequestWhenIsbnConflict() throws Exception {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
                .isbn("978-1")
                .title("Some Other Title")
                .author("Someone Else")
                .build();

        given(bookService.createBook(any(CreateBookRequest.class)))
                .willThrow(new BusinessException("ISBN already exists with different title/author"));

        // When
        // Then
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("ISBN already exists with different title/author"))
                .andExpect(jsonPath("$.path").value("/api/books"));
    }

    @Test
    void getBooksSuccessful() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        BookResponse book1 = BookResponse.builder()
                .id(1L)
                .isbn("978-1")
                .title("Clean Code")
                .author("Robert C. Martin")
                .bookStatus(BookStatus.AVAILABLE)
                .build();

        BookResponse book2 = BookResponse.builder()
                .id(2L)
                .isbn("978-2")
                .title("Effective Java")
                .author("Joshua Bloch")
                .bookStatus(BookStatus.BORROWED)
                .build();

        Page<BookResponse> page = new PageImpl<>(List.of(book1, book2), pageable, 2);

        given(bookService.getBooks(any(Pageable.class)))
                .willReturn(page);

        // When
        // Then
        mockMvc.perform(get("/api/books")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].isbn").value("978-1"))
                .andExpect(jsonPath("$.content[0].bookStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[1].isbn").value("978-2"))
                .andExpect(jsonPath("$.content[1].bookStatus").value("BORROWED"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

}