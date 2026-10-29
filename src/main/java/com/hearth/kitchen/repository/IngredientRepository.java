package com.hearth.kitchen.repository;

import com.hearth.kitchen.model.Ingredient;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingredient i where i.id in :ids order by i.id")
    List<Ingredient> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    List<Ingredient> findAllByOrderByNameAsc();
}
