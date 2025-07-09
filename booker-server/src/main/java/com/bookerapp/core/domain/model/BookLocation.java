package com.bookerapp.core.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookLocation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private Floor floor;

    public BookLocation(Floor floor) {
        this.floor = floor;
    }

    public static BookLocation of(Floor floor) {
        return new BookLocation(floor);
    }

    @Override
    public String toString() {
        return String.format("%d층", floor.getValue());
    }
}
