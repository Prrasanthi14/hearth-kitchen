package com.hearth.kitchen.dto;

import java.math.BigDecimal;

public record Shortage(
    Long ingredientId,
    String ingredientName,
    BigDecimal required,
    BigDecimal available,
    BigDecimal shortfall,
    String baseUnit,
    String displayUnit,
    BigDecimal displayFactor
) {
}
