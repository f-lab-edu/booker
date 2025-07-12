package com.bookerapp.core.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookOrderActionDto {

    @Size(max = 1000, message = "관리자 코멘트는 1000자를 초과할 수 없습니다")
    private String comments;

    public BookOrderActionDto(String comments) {
        this.comments = comments;
    }
}
