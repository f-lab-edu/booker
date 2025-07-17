package com.bookerapp.core.domain.model.entity;

import com.bookerapp.core.domain.model.Floor;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BookLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Floor floor;

    private String section;
    private String shelf;

    // 도서 위치 객체를 생성 복잡성 줄이기 위해 팩토리 메서드 사용
    // 기본 구역(A)과 서가(1)가 설정
    public static BookLocation of(Floor floor) {
        BookLocation bookLocation = new BookLocation();
        bookLocation.setFloor(floor);
        bookLocation.setSection("A");
        bookLocation.setShelf("1");
        return bookLocation;
    }

    public void updateFloor(Floor newFloor) {
        this.floor = newFloor;
    }
}
