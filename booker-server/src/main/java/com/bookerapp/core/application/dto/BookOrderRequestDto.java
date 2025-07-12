package com.bookerapp.core.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookOrderRequestDto {

    @NotBlank(message = "도서명은 필수입니다")
    @Size(max = 30, message = "도서명은 30자를 초과할 수 없습니다")
    private String title;

    @Size(max = 30, message = "저자명은 30자를 초과할 수 없습니다")
    private String author;

    @Size(max = 30, message = "출판사명은 30자를 초과할 수 없습니다")
    private String publisher;

    @Size(max = 20, message = "ISBN은 20자를 초과할 수 없습니다")
    private String isbn;

    public BookOrderRequestDto(String title, String author, String publisher, String isbn) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
    }
}
