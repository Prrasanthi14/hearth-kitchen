package com.hearth.kitchen.dto;

import com.hearth.kitchen.model.Ingredient;
import java.math.BigDecimal;

public record RequirementLine(
    Ingredient ingredient,
    BigDecimal required,
    BigDecimal available,
    BigDecimal shortfall
) {
    public boolean hasShortage() {
        return shortfall != null && shortfall.compareTo(BigDecimal.ZERO) > 0;
    }
}
