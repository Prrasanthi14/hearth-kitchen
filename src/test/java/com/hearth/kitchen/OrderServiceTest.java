package com.hearth.kitchen;

import com.hearth.kitchen.dto.CreateOrderForm;
import com.hearth.kitchen.dto.Shortage;
import com.hearth.kitchen.exception.InsufficientStockException;
import com.hearth.kitchen.exception.InvalidStatusException;
import com.hearth.kitchen.model.Ingredient;
import com.hearth.kitchen.model.MovementType;
import com.hearth.kitchen.model.OrderStatus;
import com.hearth.kitchen.model.ProductionOrder;
import com.hearth.kitchen.model.Recipe;
import com.hearth.kitchen.model.StockMovement;
import com.hearth.kitchen.repository.IngredientRepository;
import com.hearth.kitchen.repository.RecipeRepository;
import com.hearth.kitchen.service.OrderService;
import com.hearth.kitchen.service.StockService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class OrderServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockService stockService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM stock_movement");
        jdbcTemplate.execute("DELETE FROM order_requirement");
        jdbcTemplate.execute("DELETE FROM production_order");
        jdbcTemplate.execute("UPDATE ingredient SET reserved = 0");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 50000 WHERE name = 'Flour'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 20000 WHERE name = 'Sugar'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 8000 WHERE name = 'Butter'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 120 WHERE name = 'Eggs'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 15000 WHERE name = 'Milk'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 500 WHERE name = 'Yeast'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 5000 WHERE name = 'Salt'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 30000 WHERE name = 'Tomatoes'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 5000 WHERE name = 'Olive oil'");
        jdbcTemplate.execute("UPDATE ingredient SET on_hand = 2000 WHERE name = 'Garlic'");
    }

    private Ingredient findIngredientByName(String name) {
        return ingredientRepository.findAllByOrderByNameAsc().stream()
                .filter(i -> i.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ingredient not found: " + name));
    }

    private Recipe findRecipeByName(String name) {
        return recipeRepository.findByName(name)
                .orElseThrow(() -> new AssertionError("Recipe not found: " + name));
    }

    @Test
    @DisplayName("a) releaseSucceeds_reservesStockAndWritesMovements")
    void releaseSucceeds_reservesStockAndWritesMovements() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order = orderService.createOrder(new CreateOrderForm(brioche.getId(), 2));

        ProductionOrder released = orderService.release(order.getId());
        assertThat(released.getStatus()).isEqualTo(OrderStatus.RELEASED);

        Ingredient butter = findIngredientByName("Butter");
        assertThat(butter.getReserved()).isEqualByComparingTo(new BigDecimal("2400"));
        assertThat(butter.getOnHand()).isEqualByComparingTo(new BigDecimal("8000"));

        List<StockMovement> movements = orderService.getMovementsForOrder(order.getId());
        assertThat(movements).hasSize(7);
        assertThat(movements).allMatch(m -> m.getMovementType() == MovementType.RESERVE);
    }

    @Test
    @DisplayName("b) releaseShort_reservesNothing_andListsAllShortages")
    void releaseShort_reservesNothing_andListsAllShortages() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order = orderService.createOrder(new CreateOrderForm(brioche.getId(), 7));

        assertThatThrownBy(() -> orderService.release(order.getId()))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(ex -> {
                    InsufficientStockException ise = (InsufficientStockException) ex;
                    List<Shortage> shortages = ise.getShortages();
                    assertThat(shortages).hasSize(2);

                    Shortage butterShortage = shortages.stream()
                            .filter(s -> s.ingredientName().equals("Butter"))
                            .findFirst().orElseThrow();
                    assertThat(butterShortage.required()).isEqualByComparingTo(new BigDecimal("8400"));
                    assertThat(butterShortage.available()).isEqualByComparingTo(new BigDecimal("8000"));
                    assertThat(butterShortage.shortfall()).isEqualByComparingTo(new BigDecimal("400"));

                    Shortage eggsShortage = shortages.stream()
                            .filter(s -> s.ingredientName().equals("Eggs"))
                            .findFirst().orElseThrow();
                    assertThat(eggsShortage.required()).isEqualByComparingTo(new BigDecimal("126"));
                    assertThat(eggsShortage.available()).isEqualByComparingTo(new BigDecimal("120"));
                    assertThat(eggsShortage.shortfall()).isEqualByComparingTo(new BigDecimal("6"));
                });

        // Assert all reserved still 0
        List<Ingredient> ingredients = ingredientRepository.findAllByOrderByNameAsc();
        for (Ingredient ing : ingredients) {
            assertThat(ing.getReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        // Zero movements written
        List<StockMovement> movements = orderService.getMovementsForOrder(order.getId());
        assertThat(movements).isEmpty();

        // Status still DRAFT
        ProductionOrder currentOrder = orderService.getOrder(order.getId());
        assertThat(currentOrder.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    @DisplayName("c) reservationReducesAvailability")
    void reservationReducesAvailability() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order1 = orderService.createOrder(new CreateOrderForm(brioche.getId(), 4));
        orderService.release(order1.getId());

        ProductionOrder order2 = orderService.createOrder(new CreateOrderForm(brioche.getId(), 4));
        assertThatThrownBy(() -> orderService.release(order2.getId()))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(ex -> {
                    InsufficientStockException ise = (InsufficientStockException) ex;
                    List<Shortage> shortages = ise.getShortages();
                    assertThat(shortages).hasSize(2);

                    Shortage butter = shortages.stream()
                            .filter(s -> s.ingredientName().equals("Butter"))
                            .findFirst().orElseThrow();
                    assertThat(butter.required()).isEqualByComparingTo(new BigDecimal("4800"));
                    assertThat(butter.available()).isEqualByComparingTo(new BigDecimal("3200"));
                    assertThat(butter.shortfall()).isEqualByComparingTo(new BigDecimal("1600"));

                    Shortage eggs = shortages.stream()
                            .filter(s -> s.ingredientName().equals("Eggs"))
                            .findFirst().orElseThrow();
                    assertThat(eggs.required()).isEqualByComparingTo(new BigDecimal("72"));
                    assertThat(eggs.available()).isEqualByComparingTo(new BigDecimal("48"));
                    assertThat(eggs.shortfall()).isEqualByComparingTo(new BigDecimal("24"));
                });
    }

    @Test
    @DisplayName("d) complete_consumesReservedStock")
    void complete_consumesReservedStock() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order = orderService.createOrder(new CreateOrderForm(brioche.getId(), 2));
        orderService.release(order.getId());

        ProductionOrder completed = orderService.complete(order.getId());
        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        Ingredient butter = findIngredientByName("Butter");
        assertThat(butter.getOnHand()).isEqualByComparingTo(new BigDecimal("5600"));
        assertThat(butter.getReserved()).isEqualByComparingTo(BigDecimal.ZERO);

        List<StockMovement> movements = orderService.getMovementsForOrder(order.getId());
        assertThat(movements).hasSize(14);
        long consumeCount = movements.stream().filter(m -> m.getMovementType() == MovementType.CONSUME).count();
        assertThat(consumeCount).isEqualTo(7);
    }

    @Test
    @DisplayName("e) cancelReleased_freesReservedStock")
    void cancelReleased_freesReservedStock() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order = orderService.createOrder(new CreateOrderForm(brioche.getId(), 2));
        orderService.release(order.getId());

        ProductionOrder cancelled = orderService.cancel(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Ingredient butter = findIngredientByName("Butter");
        assertThat(butter.getReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(butter.getOnHand()).isEqualByComparingTo(new BigDecimal("8000"));

        List<StockMovement> movements = orderService.getMovementsForOrder(order.getId());
        assertThat(movements).hasSize(14);
        long unreserveCount = movements.stream().filter(m -> m.getMovementType() == MovementType.UNRESERVE).count();
        assertThat(unreserveCount).isEqualTo(7);
    }

    @Test
    @DisplayName("f) cancelDraft_writesNoMovements")
    void cancelDraft_writesNoMovements() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order = orderService.createOrder(new CreateOrderForm(brioche.getId(), 1));

        ProductionOrder cancelled = orderService.cancel(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        List<StockMovement> movements = orderService.getMovementsForOrder(order.getId());
        assertThat(movements).isEmpty();
    }

    @Test
    @DisplayName("g) invalidTransitions_areRejected")
    void invalidTransitions_areRejected() {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order = orderService.createOrder(new CreateOrderForm(brioche.getId(), 1));

        // Complete a DRAFT -> InvalidStatusException
        assertThatThrownBy(() -> orderService.complete(order.getId()))
                .isInstanceOf(InvalidStatusException.class);

        // Release order, then complete it
        orderService.release(order.getId());
        orderService.complete(order.getId());

        // Release a COMPLETED order -> InvalidStatusException
        assertThatThrownBy(() -> orderService.release(order.getId()))
                .isInstanceOf(InvalidStatusException.class);
    }

    @Test
    @DisplayName("h) concurrentReleases_onlyOneWins")
    void concurrentReleases_onlyOneWins() throws Exception {
        Recipe brioche = findRecipeByName("Brioche loaf");
        ProductionOrder order1 = orderService.createOrder(new CreateOrderForm(brioche.getId(), 4));
        ProductionOrder order2 = orderService.createOrder(new CreateOrderForm(brioche.getId(), 4));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Boolean> task1 = () -> {
            startLatch.await();
            try {
                orderService.release(order1.getId());
                return true;
            } catch (InsufficientStockException ex) {
                return false;
            }
        };

        Callable<Boolean> task2 = () -> {
            startLatch.await();
            try {
                orderService.release(order2.getId());
                return true;
            } catch (InsufficientStockException ex) {
                return false;
            }
        };

        Future<Boolean> future1 = executor.submit(task1);
        Future<Boolean> future2 = executor.submit(task2);

        // Trigger both threads simultaneously
        startLatch.countDown();

        boolean result1 = future1.get(10, TimeUnit.SECONDS);
        boolean result2 = future2.get(10, TimeUnit.SECONDS);

        executor.shutdown();

        // Exactly one should succeed and one should fail
        assertThat(result1 ^ result2).isTrue();

        ProductionOrder finalOrder1 = orderService.getOrder(order1.getId());
        ProductionOrder finalOrder2 = orderService.getOrder(order2.getId());

        int releasedCount = (finalOrder1.getStatus() == OrderStatus.RELEASED ? 1 : 0)
                + (finalOrder2.getStatus() == OrderStatus.RELEASED ? 1 : 0);
        int draftCount = (finalOrder1.getStatus() == OrderStatus.DRAFT ? 1 : 0)
                + (finalOrder2.getStatus() == OrderStatus.DRAFT ? 1 : 0);

        assertThat(releasedCount).isEqualTo(1);
        assertThat(draftCount).isEqualTo(1);

        // Butter reserved exactly 4800, reserved never exceeds on_hand
        Ingredient butter = findIngredientByName("Butter");
        assertThat(butter.getReserved()).isEqualByComparingTo(new BigDecimal("4800"));
        assertThat(butter.getOnHand().compareTo(butter.getReserved())).isGreaterThanOrEqualTo(0);
    }
}
