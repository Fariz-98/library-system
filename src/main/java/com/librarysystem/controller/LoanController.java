package com.librarysystem.controller;

import com.librarysystem.dto.response.LoanResponse;
import com.librarysystem.exception.dto.ErrorResponse;
import com.librarysystem.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Loans", description = "Operations related to borrowing and returning books")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/borrowers/{borrowerId}/borrow/{bookId}")
    @Operation(summary = "Borrow a specific book copy on behalf of a borrower")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Borrow successful",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Borrower or book not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Book is already borrowed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
    })
    public ResponseEntity<LoanResponse> borrowBook(@PathVariable Long borrowerId, @PathVariable Long bookId) {
        LoanResponse response = loanService.borrowBook(borrowerId, bookId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/borrowers/{borrowerId}/return/{bookId}")
    @Operation(summary = "Return a borrowed book on behalf of a borrower")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Return successful",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation or business error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Borrower or book not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<LoanResponse> returnBook(@PathVariable Long borrowerId, @PathVariable Long bookId) {
        LoanResponse response = loanService.returnBook(borrowerId, bookId);
        return ResponseEntity.ok(response);
    }

}
