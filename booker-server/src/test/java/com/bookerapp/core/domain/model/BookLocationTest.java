package com.bookerapp.core.domain.model;

import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.enums.Floor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookLocationTest {

    @Test
    @DisplayName("도서 위치를 생성할 수 있다")
    void shouldCreateBookLocation() {
        // given & when
        BookLocation location = BookLocation.of(Floor.FOURTH);

        // then
        assertThat(location.getFloor()).isEqualTo(Floor.FOURTH);
        assertThat(location.getFloor().getValue()).isEqualTo(4);
        assertThat(location.getSection()).isEqualTo("A");
        assertThat(location.getShelf()).isEqualTo("1");
    }

    @Test
    @DisplayName("도서 위치의 층을 변경할 수 있다")
    void shouldUpdateFloor() {
        // given
        Floor floor = Floor.TWELFTH;
        BookLocation location = BookLocation.of(floor);

        // when
        Floor newFloor = Floor.FOURTH;
        location.updateFloor(newFloor);

        // then
        assertThat(location.getFloor()).isEqualTo(Floor.FOURTH);
        assertThat(location.getFloor().getValue()).isEqualTo(4);
    }

    @Test
    @DisplayName("도서 위치는 기본 구역과 서가 정보를 가진다")
    void shouldHaveDefaultSectionAndShelf() {
        // given & when
        BookLocation location = BookLocation.of(Floor.FOURTH);

        // then
        assertThat(location.getSection()).isEqualTo("A");
        assertThat(location.getShelf()).isEqualTo("1");
    }
}
