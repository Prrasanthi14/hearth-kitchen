package com.hearth.kitchen.exception;

import com.hearth.kitchen.dto.Shortage;
import java.util.Collections;
import java.util.List;

public class InsufficientStockException extends RuntimeException {

    private final List<Shortage> shortages;

    public InsufficientStockException(List<Shortage> shortages) {
        super("Insufficient stock for " + (shortages != null ? shortages.size() : 0) + " ingredient(s)");
        this.shortages = shortages != null ? List.copyOf(shortages) : Collections.emptyList();
    }

    public List<Shortage> getShortages() {
        return shortages;
    }
}
