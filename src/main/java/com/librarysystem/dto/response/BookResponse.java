package com.librarysystem.dto.response;

import com.librarysystem.entity.enums.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private BookStatus bookStatus;

}
