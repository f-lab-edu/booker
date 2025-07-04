package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.entity.BookStatus;
import com.bookerapp.core.domain.model.entity.Floor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void 유효한_요청은_검증에_성공한다() {
        // given
        BookDto.Request request = new BookDto.Request();
        request.setTitle("테스트 도서");
        request.setAuthor("테스트 저자");
        request.setPublisher("테스트 출판사");
        request.setIsbn("9788956746425");
        request.setCoverImageUrl("http://example.com/cover.jpg");
        request.setLocation(BookLocation.of(Floor.FOURTH));

        // when
        Set<ConstraintViolation<BookDto.Request>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void 필수_필드가_없는_요청은_검증에_실패한다() {
        // given
        BookDto.Request request = new BookDto.Request();

        // when
        Set<ConstraintViolation<BookDto.Request>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .extracting("message")
                .containsExactlyInAnyOrder(
                        "제목은 필수입니다",
                        "저자는 필수입니다",
                        "출판사는 필수입니다",
                        "ISBN은 필수입니다"
                );
    }

    @Test
    void 도서_엔티티를_응답_DTO로_변환할_수_있다() {
        // given
        Book book = new Book();
        book.setTitle("테스트 도서");
        book.setAuthor("테스트 저자");
        book.setPublisher("테스트 출판사");
        book.setIsbn("9788956746425");
        book.setCoverImageUrl("http://example.com/cover.jpg");
        book.setStatus(BookStatus.AVAILABLE);
        book.setLocation(BookLocation.of(Floor.FOURTH));

        // when
        BookDto.Response response = BookDto.Response.from(book);

        // then
        assertThat(response.getTitle()).isEqualTo("테스트 도서");
        assertThat(response.getAuthor()).isEqualTo("테스트 저자");
        assertThat(response.getPublisher()).isEqualTo("테스트 출판사");
        assertThat(response.getIsbn()).isEqualTo("9788956746425");
        assertThat(response.getCoverImageUrl()).isEqualTo("http://example.com/cover.jpg");
        assertThat(response.getStatus()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(response.getLocation().getFloor()).isEqualTo(Floor.FOURTH);
        assertThat(response.getLocationDisplay()).isEqualTo("4층 A구역 1번 서가");
    }
} 