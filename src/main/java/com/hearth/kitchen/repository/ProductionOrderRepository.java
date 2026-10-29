package com.hearth.kitchen.repository;

import com.hearth.kitchen.model.OrderStatus;
import com.hearth.kitchen.model.ProductionOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ProductionOrder o where o.id = :id")
    Optional<ProductionOrder> findByIdForUpdate(@Param("id") Long id);

    /** Eagerly fetches recipe so the detail page can access order.recipe.name outside the transaction. */
    @Query("select o from ProductionOrder o join fetch o.recipe where o.id = :id")
    Optional<ProductionOrder> findByIdWithRecipe(@Param("id") Long id);

    @Query("select o from ProductionOrder o join fetch o.recipe where o.status = :status order by o.createdAt desc")
    List<ProductionOrder> findByStatusOrderByCreatedAtDesc(@Param("status") OrderStatus status);

    @Query("select o from ProductionOrder o join fetch o.recipe order by o.createdAt desc")
    List<ProductionOrder> findAllByOrderByCreatedAtDesc();
}
