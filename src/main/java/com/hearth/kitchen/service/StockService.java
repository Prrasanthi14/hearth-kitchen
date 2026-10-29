package com.hearth.kitchen.service;

import com.hearth.kitchen.exception.NotFoundException;
import com.hearth.kitchen.model.Ingredient;
import com.hearth.kitchen.model.StockMovement;
import com.hearth.kitchen.repository.IngredientRepository;
import com.hearth.kitchen.repository.StockMovementRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {

    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockService(IngredientRepository ingredientRepository, StockMovementRepository stockMovementRepository) {
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<Ingredient> listIngredients() {
        return ingredientRepository.findAllByOrderByNameAsc();
    }

    public Ingredient getIngredient(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient not found: " + id));
    }

    public List<StockMovement> getMovementsForIngredient(Long id) {
        return stockMovementRepository.findByIngredientIdOrderByCreatedAtDesc(id);
    }
}
