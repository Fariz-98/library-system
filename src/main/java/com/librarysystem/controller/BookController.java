package com.librarysystem.controller;

import com.librarysystem.dto.request.CreateBookRequest;
import com.librarysystem.dto.response.BookResponse;
import com.librarysystem.exception.dto.ErrorResponse;
import com.librarysystem.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Operations related to books and borrowing")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @Operation(summary = "Register a new copy")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Book created",
                    content = @Content(schema = @Schema(implementation = BookResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation or business error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse response = bookService.createBook(request);
        URI location = URI.create("/api/books/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all books")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of books",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookResponse.class)))
            )
    })
    public ResponseEntity<Page<BookResponse>> getBooks(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookResponse> books = bookService.getBooks(pageable);
        return ResponseEntity.ok(books);
    }

}
