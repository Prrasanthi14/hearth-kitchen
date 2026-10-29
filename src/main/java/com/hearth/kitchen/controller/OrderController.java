package com.hearth.kitchen.controller;

import com.hearth.kitchen.dto.CreateOrderForm;
import com.hearth.kitchen.dto.RequirementLine;
import com.hearth.kitchen.exception.InsufficientStockException;
import com.hearth.kitchen.exception.InvalidStatusException;
import com.hearth.kitchen.model.OrderStatus;
import com.hearth.kitchen.model.ProductionOrder;
import com.hearth.kitchen.model.StockMovement;
import com.hearth.kitchen.repository.RecipeRepository;
import com.hearth.kitchen.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final RecipeRepository recipeRepository;

    public OrderController(OrderService orderService, RecipeRepository recipeRepository) {
        this.orderService = orderService;
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/orders")
    public String listOrders(@RequestParam(value = "status", required = false) OrderStatus status,
                             Model model) {
        List<ProductionOrder> orders = orderService.listOrders(status);
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("recipes", recipeRepository.findAll());
        if (!model.containsAttribute("createOrderForm")) {
            model.addAttribute("createOrderForm", new CreateOrderForm());
        }
        return "orders";
    }

    @PostMapping("/orders")
    public String createOrder(@Valid @ModelAttribute("createOrderForm") CreateOrderForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("orders", orderService.listOrders(null));
            model.addAttribute("currentStatus", null);
            model.addAttribute("allStatuses", OrderStatus.values());
            model.addAttribute("recipes", recipeRepository.findAll());
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("errorMessage", errorMsg);
            return "orders";
        }

        ProductionOrder newOrder = orderService.createOrder(form);
        redirectAttributes.addFlashAttribute("successMessage", "Order created as Draft");
        return "redirect:/orders/" + newOrder.getId();
    }

    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable("id") Long id, Model model) {
        ProductionOrder order = orderService.getOrder(id);
        List<RequirementLine> requirementLines = orderService.getRequirementLines(order);
        List<StockMovement> movements = orderService.getMovementsForOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("requirements", requirementLines);
        model.addAttribute("movements", movements);
        return "order";
    }

    @PostMapping("/orders/{id}/release")
    public String releaseOrder(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.release(id);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " released successfully");
        } catch (InsufficientStockException ex) {
            redirectAttributes.addFlashAttribute("shortages", ex.getShortages());
        } catch (InvalidStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/complete")
    public String completeOrder(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.complete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " completed successfully");
        } catch (InvalidStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " cancelled successfully");
        } catch (InvalidStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
