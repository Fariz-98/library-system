package com.librarysystem.controller;

import com.librarysystem.dto.request.CreateBorrowerRequest;
import com.librarysystem.dto.response.BorrowerResponse;
import com.librarysystem.exception.dto.ErrorResponse;
import com.librarysystem.service.BorrowerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/borrowers")
@Tag(name = "Borrowers", description = "Operations related to library borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping
    @Operation(summary = "Register a new borrower")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Borrower created",
                    content = @Content(schema = @Schema(implementation = BorrowerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation or business error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email is taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
    })
    public ResponseEntity<BorrowerResponse> createBorrower(@Valid @RequestBody CreateBorrowerRequest request) {
        BorrowerResponse response = borrowerService.createBorrower(request);
        URI location = URI.create("/api/borrowers/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }
}
