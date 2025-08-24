package com.bookerapp.core.domain.model;

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
}
