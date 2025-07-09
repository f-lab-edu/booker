package com.bookerapp.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class BookLocationTest {

    @Test
    @DisplayName("BookLocation 생성 테스트")
    void createBookLocation() {
        // given & when
        BookLocation location = BookLocation.of(Floor.FOURTH);

        // then
        assertThat(location.getFloor()).isEqualTo(Floor.FOURTH);
        assertThat(location.getFloor().getValue()).isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(Floor.class)
    @DisplayName("모든 Floor enum 값으로 BookLocation 생성 가능")
    void createBookLocationWithAllFloors(Floor floor) {
        // given & when
        BookLocation location = BookLocation.of(floor);

        // then
        assertThat(location.getFloor()).isEqualTo(floor);
    }

    @Test
    @DisplayName("도서 위치 정보를 문자열로 표현할 수 있다")
    void shouldFormatLocationAsString() {
        // given
        BookLocation location = BookLocation.of(Floor.FOURTH);

        // when
        String locationString = location.toString();

        // then
        assertThat(locationString).isEqualTo("4층");
    }
}
