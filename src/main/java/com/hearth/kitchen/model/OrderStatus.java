package com.hearth.kitchen.model;

public enum OrderStatus {
    DRAFT,
    RELEASED,
    COMPLETED,
    CANCELLED;

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case DRAFT -> next == RELEASED || next == CANCELLED;
            case RELEASED -> next == COMPLETED || next == CANCELLED;
            default -> false;
        };
    }
}
