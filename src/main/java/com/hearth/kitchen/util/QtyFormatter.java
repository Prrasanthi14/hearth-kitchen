package com.hearth.kitchen.util;

import com.hearth.kitchen.dto.Shortage;
import com.hearth.kitchen.model.Ingredient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component("qty")
public class QtyFormatter {

    /**
     * Formats a quantity in base units into display units (e.g. 7200 g -> "7.2 kg").
     * Max 3 decimal places with trailing zeros stripped.
     */
    public String format(BigDecimal baseQty, Ingredient ing) {
        if (baseQty == null) {
            baseQty = BigDecimal.ZERO;
        }
        if (ing == null) {
            return strip(baseQty).toPlainString();
        }
        return format(baseQty, ing.getDisplayUnit(), ing.getDisplayFactor());
    }

    /**
     * Formats a quantity with both display and base units, e.g. "7.2 kg (7200 g)".
     * If display unit equals base unit, returns e.g. "120 pcs".
     */
    public String formatBoth(BigDecimal baseQty, Ingredient ing) {
        if (baseQty == null) {
            baseQty = BigDecimal.ZERO;
        }
        if (ing == null) {
            return strip(baseQty).toPlainString();
        }
        return formatBoth(baseQty, ing.getBaseUnit(), ing.getDisplayUnit(), ing.getDisplayFactor());
    }

    public String format(BigDecimal baseQty, Shortage shortage) {
        if (baseQty == null) {
            baseQty = BigDecimal.ZERO;
        }
        if (shortage == null) {
            return strip(baseQty).toPlainString();
        }
        return format(baseQty, shortage.displayUnit(), shortage.displayFactor());
    }

    public String formatBoth(BigDecimal baseQty, Shortage shortage) {
        if (baseQty == null) {
            baseQty = BigDecimal.ZERO;
        }
        if (shortage == null) {
            return strip(baseQty).toPlainString();
        }
        return formatBoth(baseQty, shortage.baseUnit(), shortage.displayUnit(), shortage.displayFactor());
    }

    public String format(BigDecimal baseQty, String displayUnit, BigDecimal displayFactor) {
        if (baseQty == null) {
            baseQty = BigDecimal.ZERO;
        }
        BigDecimal factor = (displayFactor != null && displayFactor.compareTo(BigDecimal.ZERO) > 0)
                ? displayFactor
                : BigDecimal.ONE;

        BigDecimal displayQty = baseQty.divide(factor, 3, RoundingMode.HALF_UP);
        displayQty = strip(displayQty);

        String unit = displayUnit != null ? displayUnit : "";
        return displayQty.toPlainString() + (unit.isEmpty() ? "" : " " + unit);
    }

    public String formatBoth(BigDecimal baseQty, String baseUnit, String displayUnit, BigDecimal displayFactor) {
        if (baseQty == null) {
            baseQty = BigDecimal.ZERO;
        }
        String bUnit = baseUnit != null ? baseUnit : "";
        String dUnit = displayUnit != null ? displayUnit : "";

        if (dUnit.equalsIgnoreCase(bUnit)) {
            return format(baseQty, dUnit, displayFactor);
        }

        String displayFormatted = format(baseQty, dUnit, displayFactor);
        String baseFormatted = strip(baseQty).toPlainString();
        return displayFormatted + " (" + baseFormatted + (bUnit.isEmpty() ? "" : " " + bUnit) + ")";
    }

    private BigDecimal strip(BigDecimal val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal stripped = val.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0, RoundingMode.UNNECESSARY);
        }
        return stripped;
    }
}
