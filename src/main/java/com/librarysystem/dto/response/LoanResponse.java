package com.librarysystem.dto.response;

import com.librarysystem.entity.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {

    private Long id;

    private Long bookId;
    private String bookIsbn;
    private String bookTitle;
    private String bookAuthor;

    private Long borrowerId;
    private String borrowerName;
    private String borrowerEmail;

    private LoanStatus status;

    private LocalDateTime borrowedAt;
    private LocalDateTime returnedAt;

}
