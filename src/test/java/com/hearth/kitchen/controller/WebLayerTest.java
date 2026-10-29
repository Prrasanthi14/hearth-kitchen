package com.hearth.kitchen.controller;

import com.hearth.kitchen.model.Ingredient;
import com.hearth.kitchen.model.OrderStatus;
import com.hearth.kitchen.model.ProductionOrder;
import com.hearth.kitchen.model.Recipe;
import com.hearth.kitchen.repository.IngredientRepository;
import com.hearth.kitchen.repository.ProductionOrderRepository;
import com.hearth.kitchen.repository.RecipeRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebLayerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ProductionOrderRepository productionOrderRepository;

    @Test
    @DisplayName("GET / redirects to /stock")
    void testRootRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/stock"));
    }

    @Test
    @DisplayName("GET /stock displays stock table")
    void testStockPage() throws Exception {
        mockMvc.perform(get("/stock"))
                .andExpect(status().isOk())
                .andExpect(view().name("stock"))
                .andExpect(model().attributeExists("ingredients"))
                .andExpect(model().attribute("ingredients", notNullValue()));
    }

    @Test
    @DisplayName("GET /stock/{id} displays ingredient detail and movements")
    void testIngredientPage() throws Exception {
        Ingredient flour = ingredientRepository.findAllByOrderByNameAsc().stream()
                .filter(i -> i.getName().equals("Flour")).findFirst().orElseThrow();

        mockMvc.perform(get("/stock/" + flour.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredient"))
                .andExpect(model().attribute("ingredient", notNullValue()))
                .andExpect(model().attributeExists("movements"));
    }

    @Test
    @DisplayName("GET /stock/99999 returns 404 error page")
    void testIngredientNotFound() throws Exception {
        mockMvc.perform(get("/stock/99999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /orders displays order list and create form")
    void testOrdersPage() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("recipes"))
                .andExpect(model().attributeExists("allStatuses"));
    }

    @Test
    @DisplayName("GET /orders?status=DRAFT filters orders")
    void testOrdersFilter() throws Exception {
        mockMvc.perform(get("/orders").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("currentStatus", OrderStatus.DRAFT));
    }

    @Test
    @DisplayName("POST /orders creates order and redirects to /orders/{id}")
    void testCreateOrderPostRedirectGet() throws Exception {
        Recipe recipe = recipeRepository.findByName("Brioche loaf").orElseThrow();

        mockMvc.perform(post("/orders")
                        .param("recipeId", recipe.getId().toString())
                        .param("batches", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Order created as Draft"));
    }

    @Test
    @DisplayName("POST /orders with invalid batches re-shows orders page")
    void testCreateOrderValidationError() throws Exception {
        Recipe recipe = recipeRepository.findByName("Brioche loaf").orElseThrow();

        mockMvc.perform(post("/orders")
                        .param("recipeId", recipe.getId().toString())
                        .param("batches", "0")) // validation violation (< 1)
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /orders/{id}/release with enough stock redirects with success")
    void testReleaseOrderSuccess() throws Exception {
        Recipe recipe = recipeRepository.findByName("Brioche loaf").orElseThrow();
        ProductionOrder order = productionOrderRepository.save(new ProductionOrder(recipe, 1));

        mockMvc.perform(post("/orders/" + order.getId() + "/release"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/orders/" + order.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /orders/{id}/release with insufficient stock redirects with shortages flash attribute")
    void testReleaseOrderShortage() throws Exception {
        Recipe recipe = recipeRepository.findByName("Brioche loaf").orElseThrow();
        ProductionOrder order = productionOrderRepository.save(new ProductionOrder(recipe, 100)); // needs more than available

        mockMvc.perform(post("/orders/" + order.getId() + "/release"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/orders/" + order.getId()))
                .andExpect(flash().attributeExists("shortages"));
    }

    @Test
    @DisplayName("POST /orders/{id}/complete completes released order")
    void testCompleteOrder() throws Exception {
        Recipe recipe = recipeRepository.findByName("Brioche loaf").orElseThrow();
        ProductionOrder order = productionOrderRepository.save(new ProductionOrder(recipe, 1));

        // Release first
        mockMvc.perform(post("/orders/" + order.getId() + "/release"));

        // Now complete
        mockMvc.perform(post("/orders/" + order.getId() + "/complete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/orders/" + order.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel cancels order")
    void testCancelOrder() throws Exception {
        Recipe recipe = recipeRepository.findByName("Brioche loaf").orElseThrow();
        ProductionOrder order = productionOrderRepository.save(new ProductionOrder(recipe, 1));

        mockMvc.perform(post("/orders/" + order.getId() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/orders/" + order.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
