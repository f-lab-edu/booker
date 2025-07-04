package com.bookerapp.core.domain.model.entity;

import lombok.Getter;

@Getter
public enum Floor {
    FOURTH(4),
    TWELFTH(12);

    private final int number;

    Floor(int number) {
        this.number = number;
    }

    public int getValue() {
        return number;
    }
} 