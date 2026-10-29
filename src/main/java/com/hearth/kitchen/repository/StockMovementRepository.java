package com.hearth.kitchen.repository;

import com.hearth.kitchen.model.StockMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** Used by ingredient.html — newest first, ingredient and order fully loaded. */
    @Query("select m from StockMovement m join fetch m.ingredient join fetch m.order where m.ingredient.id = :ingredientId order by m.createdAt desc")
    List<StockMovement> findByIngredientIdOrderByCreatedAtDesc(@Param("ingredientId") Long ingredientId);

    /** Used by order.html — oldest first, ingredient and order fully loaded. */
    @Query("select m from StockMovement m join fetch m.ingredient join fetch m.order where m.order.id = :orderId order by m.createdAt asc")
    List<StockMovement> findByOrderIdOrderByCreatedAtAsc(@Param("orderId") Long orderId);
}
