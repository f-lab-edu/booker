package com.bookerapp.core.domain.model;

import com.bookerapp.core.domain.exception.InvalidFloorException;

public enum Floor {
    FOURTH(4),
    TWELFTH(12);

    private final int value;

    Floor(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Floor fromValue(int value) {
        for (Floor floor : Floor.values()) {
            if (floor.value == value) {
                return floor;
            }
        }
        throw new InvalidFloorException(value);
    }
}
