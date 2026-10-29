package com.hearth.kitchen.controller;

import com.hearth.kitchen.model.Ingredient;
import com.hearth.kitchen.model.StockMovement;
import com.hearth.kitchen.service.StockService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/stock";
    }

    @GetMapping("/stock")
    public String listStock(Model model) {
        List<Ingredient> ingredients = stockService.listIngredients();
        model.addAttribute("ingredients", ingredients);
        return "stock";
    }

    @GetMapping("/stock/{id}")
    public String viewIngredient(@PathVariable("id") Long id, Model model) {
        Ingredient ingredient = stockService.getIngredient(id);
        List<StockMovement> movements = stockService.getMovementsForIngredient(id);
        model.addAttribute("ingredient", ingredient);
        model.addAttribute("movements", movements);
        return "ingredient";
    }
}
