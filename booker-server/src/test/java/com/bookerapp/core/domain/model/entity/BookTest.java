package com.bookerapp.core.domain.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    @Test
    @DisplayName("새로운 도서는 기본적으로 대출 가능 상태이다")
    void newBookShouldBeAvailable() {
        // given
        Book book = new Book();

        // when & then
        assertThat(book.getStatus()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(book.isAvailableForLoan()).isTrue();
    }

    @Test
    @DisplayName("도서 상태를 변경할 수 있다")
    void shouldUpdateBookStatus() {
        // given
        Book book = new Book();

        // when
        book.updateStatus(BookStatus.LOANED);

        // then
        assertThat(book.getStatus()).isEqualTo(BookStatus.LOANED);
        assertThat(book.isAvailableForLoan()).isFalse();
    }

    @Test
    @DisplayName("도서 위치를 변경할 수 있다")
    void shouldUpdateLocation() {
        // given
        Book book = new Book();
        book.setLocation(BookLocation.of(Floor.FOURTH));

        // when
        BookLocation newLocation = BookLocation.of(Floor.TWELFTH);
        book.setLocation(newLocation);

        // then
        assertThat(book.getLocation())
                .isNotNull()
                .satisfies(loc -> {
                    assertThat(loc.getFloor()).isEqualTo(Floor.TWELFTH);
                    assertThat(loc.getFloor().getValue()).isEqualTo(12);
                    assertThat(loc.getSection()).isEqualTo("A");
                    assertThat(loc.getShelf()).isEqualTo("1");
                });
    }

    @Test
    @DisplayName("도서를 삭제 상태로 변경할 수 있다")
    void shouldMarkAsDeleted() {
        // given
        Book book = new Book();
        assertThat(book.isDeleted()).isFalse();

        // when
        book.markAsDeleted();

        // then
        assertThat(book.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 도서를 복구할 수 있다")
    void shouldUnmarkAsDeleted() {
        // given
        Book book = new Book();
        book.markAsDeleted();
        assertThat(book.isDeleted()).isTrue();

        // when
        book.unmarkAsDeleted();

        // then
        assertThat(book.isDeleted()).isFalse();
    }
}
