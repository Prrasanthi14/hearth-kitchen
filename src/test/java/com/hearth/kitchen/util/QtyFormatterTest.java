package com.hearth.kitchen.util;

import com.hearth.kitchen.model.Ingredient;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QtyFormatterTest {

    private final QtyFormatter qty = new QtyFormatter();

    @Test
    @DisplayName("format() converts base units to display units with trailing zeros stripped")
    void testFormat() {
        Ingredient flour = new Ingredient("Flour", "g", "kg", new BigDecimal("1000"), new BigDecimal("50000"), BigDecimal.ZERO);
        assertThat(qty.format(new BigDecimal("7200"), flour)).isEqualTo("7.2 kg");
        assertThat(qty.format(new BigDecimal("50000"), flour)).isEqualTo("50 kg");
        assertThat(qty.format(new BigDecimal("1234"), flour)).isEqualTo("1.234 kg");
        assertThat(qty.format(new BigDecimal("1234.5678"), flour)).isEqualTo("1.235 kg");
    }

    @Test
    @DisplayName("formatBoth() returns 'display (base)' when units differ")
    void testFormatBothDifferentUnits() {
        Ingredient flour = new Ingredient("Flour", "g", "kg", new BigDecimal("1000"), new BigDecimal("50000"), BigDecimal.ZERO);
        assertThat(qty.formatBoth(new BigDecimal("7200"), flour)).isEqualTo("7.2 kg (7200 g)");
    }

    @Test
    @DisplayName("formatBoth() returns just display unit when display unit equals base unit")
    void testFormatBothSameUnits() {
        Ingredient eggs = new Ingredient("Eggs", "pcs", "pcs", BigDecimal.ONE, new BigDecimal("120"), BigDecimal.ZERO);
        assertThat(qty.formatBoth(new BigDecimal("120"), eggs)).isEqualTo("120 pcs");
    }
}
