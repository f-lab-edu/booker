package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.entity.BookStatus;
import com.bookerapp.core.domain.model.entity.Floor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 도서 등록 요청을 엔티티로 변환할 수 있다")
    void shouldConvertValidRequestToEntity() {
        // given
        BookDto.Request request = new BookDto.Request();
        request.setTitle("클린 코드");
        request.setAuthor("로버트 C. 마틴");
        request.setIsbn("9788966260959");
        request.setPublisher("인사이트");
        request.setCoverImageUrl("http://example.com/cover.jpg");
        request.setLocation(BookLocation.of(Floor.FOURTH));

        // when
        Book book = request.toEntity();

        // then
        assertThat(book)
            .isNotNull()
            .satisfies(b -> {
                assertThat(b.getTitle()).isEqualTo(request.getTitle());
                assertThat(b.getAuthor()).isEqualTo(request.getAuthor());
                assertThat(b.getIsbn()).isEqualTo(request.getIsbn());
                assertThat(b.getPublisher()).isEqualTo(request.getPublisher());
                assertThat(b.getCoverImageUrl()).isEqualTo(request.getCoverImageUrl());
                assertThat(b.getStatus()).isEqualTo(BookStatus.AVAILABLE);
                assertThat(b.getLocation().getFloor()).isEqualTo(request.getLocation().getFloor());
            });
    }

    @Test
    @DisplayName("필수 필드가 없는 요청은 검증에 실패한다")
    void shouldFailValidationForMissingRequiredFields() {
        // given
        BookDto.Request request = new BookDto.Request();

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations)
            .isNotEmpty()
            .hasSize(2)
            .extracting("message")
            .containsExactlyInAnyOrder(
                "제목은 필수입니다",
                "저자는 필수입니다"
            );
    }

    @Test
    @DisplayName("잘못된 ISBN 형식은 검증에 실패한다")
    void shouldFailValidationForInvalidIsbn() {
        // given
        BookDto.Request request = new BookDto.Request();
        request.setTitle("클린 코드");
        request.setAuthor("로버트 C. 마틴");
        request.setIsbn("invalid-isbn");

        // when
        var violations = validator.validate(request);

        // then
        assertThat(violations)
            .isNotEmpty()
            .hasSize(1)
            .extracting("message")
            .containsExactly("올바른 ISBN 형식이 아닙니다");
    }

    @Test
    @DisplayName("도서 엔티티를 응답 DTO로 변환할 수 있다")
    void shouldConvertEntityToResponse() {
        // given
        Book book = new Book();
        book.setId(1L);
        book.setTitle("클린 코드");
        book.setAuthor("로버트 C. 마틴");
        book.setIsbn("9788966260959");
        book.setPublisher("인사이트");
        book.setCoverImageUrl("http://example.com/cover.jpg");
        book.setStatus(BookStatus.AVAILABLE);
        book.setLocation(BookLocation.of(Floor.FOURTH));

        // when
        BookDto.Response response = BookDto.Response.from(book);

        // then
        assertThat(response)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.getId()).isEqualTo(book.getId());
                assertThat(r.getTitle()).isEqualTo(book.getTitle());
                assertThat(r.getAuthor()).isEqualTo(book.getAuthor());
                assertThat(r.getIsbn()).isEqualTo(book.getIsbn());
                assertThat(r.getPublisher()).isEqualTo(book.getPublisher());
                assertThat(r.getCoverImageUrl()).isEqualTo(book.getCoverImageUrl());
                assertThat(r.getStatus()).isEqualTo(book.getStatus());
                assertThat(r.getLocation().getFloor()).isEqualTo(book.getLocation().getFloor());
                assertThat(r.getLocationDisplay()).isEqualTo("4층");
            });
    }
} 