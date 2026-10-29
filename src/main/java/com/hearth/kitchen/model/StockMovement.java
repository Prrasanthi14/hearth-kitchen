package com.hearth.kitchen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "on_hand_after", nullable = false, precision = 14, scale = 3)
    private BigDecimal onHandAfter;

    @Column(name = "reserved_after", nullable = false, precision = 14, scale = 3)
    private BigDecimal reservedAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public StockMovement() {
    }

    public StockMovement(Ingredient ingredient, ProductionOrder order, MovementType movementType,
                         BigDecimal quantity, BigDecimal onHandAfter, BigDecimal reservedAfter) {
        this.ingredient = ingredient;
        this.order = order;
        this.movementType = movementType;
        this.quantity = quantity;
        this.onHandAfter = onHandAfter;
        this.reservedAfter = reservedAfter;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public ProductionOrder getOrder() {
        return order;
    }

    public void setOrder(ProductionOrder order) {
        this.order = order;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOnHandAfter() {
        return onHandAfter;
    }

    public void setOnHandAfter(BigDecimal onHandAfter) {
        this.onHandAfter = onHandAfter;
    }

    public BigDecimal getReservedAfter() {
        return reservedAfter;
    }

    public void setReservedAfter(BigDecimal reservedAfter) {
        this.reservedAfter = reservedAfter;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
