package com.bookerapp.core.domain.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BookLocation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private Floor floor;

    private String section;
    private String shelf;

    public static BookLocation of(Floor floor) {
        BookLocation location = new BookLocation();
        location.setFloor(floor);
        location.setSection("A");  // 기본값
        location.setShelf("1");    // 기본값
        return location;
    }

    @Override
    public String toString() {
        return String.format("%s층 %s구역 %s번 서가", floor.getNumber(), section, shelf);
    }
}
