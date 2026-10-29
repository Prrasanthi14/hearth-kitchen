package com.hearth.kitchen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "base_unit", nullable = false, length = 10)
    private String baseUnit;

    @Column(name = "display_unit", nullable = false, length = 10)
    private String displayUnit;

    @Column(name = "display_factor", nullable = false, precision = 10, scale = 3)
    private BigDecimal displayFactor;

    @Column(name = "on_hand", nullable = false, precision = 14, scale = 3)
    private BigDecimal onHand;

    @Column(name = "reserved", nullable = false, precision = 14, scale = 3)
    private BigDecimal reserved = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Ingredient() {
    }

    public Ingredient(String name, String baseUnit, String displayUnit, BigDecimal displayFactor, BigDecimal onHand, BigDecimal reserved) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.displayUnit = displayUnit;
        this.displayFactor = displayFactor;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    public BigDecimal getAvailable() {
        if (onHand == null) {
            return BigDecimal.ZERO;
        }
        if (reserved == null) {
            return onHand;
        }
        return onHand.subtract(reserved);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    public String getDisplayUnit() {
        return displayUnit;
    }

    public void setDisplayUnit(String displayUnit) {
        this.displayUnit = displayUnit;
    }

    public BigDecimal getDisplayFactor() {
        return displayFactor;
    }

    public void setDisplayFactor(BigDecimal displayFactor) {
        this.displayFactor = displayFactor;
    }

    public BigDecimal getOnHand() {
        return onHand;
    }

    public void setOnHand(BigDecimal onHand) {
        this.onHand = onHand;
    }

    public BigDecimal getReserved() {
        return reserved;
    }

    public void setReserved(BigDecimal reserved) {
        this.reserved = reserved;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
