package com.hearth.kitchen.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateOrderForm {

    @NotNull(message = "Recipe is required")
    private Long recipeId;

    @NotNull(message = "Batches count is required")
    @Min(value = 1, message = "Batches must be at least 1")
    private Integer batches = 1;

    public CreateOrderForm() {
    }

    public CreateOrderForm(Long recipeId, Integer batches) {
        this.recipeId = recipeId;
        this.batches = batches;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getBatches() {
        return batches;
    }

    public void setBatches(Integer batches) {
        this.batches = batches;
    }
}
