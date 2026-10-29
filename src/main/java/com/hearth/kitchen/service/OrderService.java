package com.hearth.kitchen.service;

import com.hearth.kitchen.dto.CreateOrderForm;
import com.hearth.kitchen.dto.RequirementLine;
import com.hearth.kitchen.dto.Shortage;
import com.hearth.kitchen.exception.InsufficientStockException;
import com.hearth.kitchen.exception.InvalidStatusException;
import com.hearth.kitchen.exception.NotFoundException;
import com.hearth.kitchen.model.Ingredient;
import com.hearth.kitchen.model.MovementType;
import com.hearth.kitchen.model.OrderRequirement;
import com.hearth.kitchen.model.OrderStatus;
import com.hearth.kitchen.model.ProductionOrder;
import com.hearth.kitchen.model.Recipe;
import com.hearth.kitchen.model.RecipeIngredient;
import com.hearth.kitchen.model.StockMovement;
import com.hearth.kitchen.repository.IngredientRepository;
import com.hearth.kitchen.repository.OrderRequirementRepository;
import com.hearth.kitchen.repository.ProductionOrderRepository;
import com.hearth.kitchen.repository.RecipeIngredientRepository;
import com.hearth.kitchen.repository.RecipeRepository;
import com.hearth.kitchen.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final OrderRequirementRepository orderRequirementRepository;
    private final StockMovementRepository stockMovementRepository;

    public OrderService(ProductionOrderRepository productionOrderRepository,
                        RecipeRepository recipeRepository,
                        RecipeIngredientRepository recipeIngredientRepository,
                        IngredientRepository ingredientRepository,
                        OrderRequirementRepository orderRequirementRepository,
                        StockMovementRepository stockMovementRepository) {
        this.productionOrderRepository = productionOrderRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.orderRequirementRepository = orderRequirementRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    /**
     * 1) Create a new production order in DRAFT status.
     */
    @Transactional
    public ProductionOrder createOrder(CreateOrderForm form) {
        Recipe recipe = recipeRepository.findById(form.getRecipeId())
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + form.getRecipeId()));

        ProductionOrder order = new ProductionOrder(recipe, form.getBatches());
        order.setStatus(OrderStatus.DRAFT);
        return productionOrderRepository.save(order);
    }

    /**
     * 2) Release a production order and reserve required ingredients.
     *
     * WHY THIS LOCKING AND ORDERING PATTERN:
     * - Atomicity (All-or-Nothing): We validate stock across ALL recipe ingredients before modifying
     *   anything. If even one ingredient is short, we throw InsufficientStockException and the entire
     *   operation rolls back, guaranteeing we never leave stock in a partially reserved state.
     * - Why Locks: Pessimistic write locks (SELECT ... FOR UPDATE) prevent race conditions where concurrent
     *   orders read the same "available" stock and both attempt to reserve it, which would breach database
     *   constraints and result in negative available inventory.
     * - Why Lock Order (Order first, then Ingredients in ASC ID order): Acquiring database locks in a
     *   strictly uniform order across all concurrent transactions guarantees there are no circular wait
     *   graphs, mathematically eliminating transaction deadlocks.
     */
    @Transactional
    public ProductionOrder release(Long orderId) {
        // Step a: Lock the order row first to prevent concurrent modifications
        ProductionOrder order = productionOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (!order.getStatus().canMoveTo(OrderStatus.RELEASED)) {
            throw new InvalidStatusException("Cannot move order " + orderId + " from " + order.getStatus() + " to " + OrderStatus.RELEASED);
        }

        // Step b: Compute required quantity per ingredient = recipe qty_per_batch * batches
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(order.getRecipe().getId());

        // Step c: Lock required ingredients in deterministic ascending ID order to prevent deadlocks
        List<Long> ingredientIds = recipeIngredients.stream()
                .map(ri -> ri.getIngredient().getId())
                .sorted()
                .toList();

        List<Ingredient> lockedIngredients = ingredientRepository.findAllByIdForUpdate(ingredientIds);
        Map<Long, Ingredient> ingredientMap = lockedIngredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        // Step d: Loop through ALL ingredients and collect EVERY shortage where available < required.
        // We do NOT stop at the first one, so the kitchen operator sees all missing items at once.
        List<Shortage> shortages = new ArrayList<>();
        for (RecipeIngredient ri : recipeIngredients) {
            Ingredient ing = ingredientMap.get(ri.getIngredient().getId());
            BigDecimal required = ri.getQtyPerBatch().multiply(BigDecimal.valueOf(order.getBatches()));
            BigDecimal available = ing.getAvailable();

            if (available.compareTo(required) < 0) {
                BigDecimal shortfall = required.subtract(available);
                shortages.add(new Shortage(
                        ing.getId(),
                        ing.getName(),
                        required,
                        available,
                        shortfall,
                        ing.getBaseUnit(),
                        ing.getDisplayUnit(),
                        ing.getDisplayFactor()
                ));
            }
        }

        // Step e: If any shortages exist, abort immediately. Nothing is changed; transaction rolls back.
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }

        // Step f: All ingredients are available. Reserve inventory, snapshot requirements, record audit ledger.
        for (RecipeIngredient ri : recipeIngredients) {
            Ingredient ing = ingredientMap.get(ri.getIngredient().getId());
            BigDecimal required = ri.getQtyPerBatch().multiply(BigDecimal.valueOf(order.getBatches()));

            // Update reserved stock
            ing.setReserved(ing.getReserved().add(required));

            // Snapshot requirement at release time
            OrderRequirement req = new OrderRequirement(order, ing, required);
            orderRequirementRepository.save(req);

            // Record immutable stock movement
            StockMovement movement = new StockMovement(
                    ing,
                    order,
                    MovementType.RESERVE,
                    required,
                    ing.getOnHand(),      // on_hand_after
                    ing.getReserved()     // reserved_after
            );
            stockMovementRepository.save(movement);
        }

        // Explicit save: pessimistic-lock JPQL queries can return entities that Hibernate's
        // dirty-checker does not track reliably — saveAll guarantees the UPDATE is flushed.
        ingredientRepository.saveAll(lockedIngredients);

        // Step g: Transition status to RELEASED and set released timestamp
        order.setStatus(OrderStatus.RELEASED);
        order.setReleasedAt(OffsetDateTime.now());
        return order;
    }

    /**
     * 3) Complete a production order: consumes reserved stock and records CONSUME movements.
     */
    @Transactional
    public ProductionOrder complete(Long orderId) {
        // Step 1: Lock the order and validate status transition
        ProductionOrder order = productionOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (!order.getStatus().canMoveTo(OrderStatus.COMPLETED)) {
            throw new InvalidStatusException("Cannot move order " + orderId + " from " + order.getStatus() + " to " + OrderStatus.COMPLETED);
        }

        // Step 2: Load the released OrderRequirements snapshot
        List<OrderRequirement> requirements = orderRequirementRepository.findByOrderId(orderId);

        // Step 3: Lock ingredients in ascending ID order
        List<Long> ingredientIds = requirements.stream()
                .map(r -> r.getIngredient().getId())
                .sorted()
                .toList();

        List<Ingredient> lockedIngredients = ingredientRepository.findAllByIdForUpdate(ingredientIds);
        Map<Long, Ingredient> ingredientMap = lockedIngredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        // Step 4: Deduct on_hand and reserved; record CONSUME movement per ingredient
        for (OrderRequirement req : requirements) {
            Ingredient ing = ingredientMap.get(req.getIngredient().getId());
            BigDecimal required = req.getRequiredQty();

            ing.setOnHand(ing.getOnHand().subtract(required));
            ing.setReserved(ing.getReserved().subtract(required));

            StockMovement movement = new StockMovement(
                    ing,
                    order,
                    MovementType.CONSUME,
                    required,
                    ing.getOnHand(),      // on_hand_after
                    ing.getReserved()     // reserved_after
            );
            stockMovementRepository.save(movement);
        }

        // Explicit save: guarantees ingredient balance changes are flushed regardless of dirty-check.
        ingredientRepository.saveAll(lockedIngredients);

        // Step 5: Mark order as COMPLETED
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(OffsetDateTime.now());
        return order;
    }

    /**
     * 4) Cancel an order: unreserves stock if previously RELEASED, or cancels directly if DRAFT.
     */
    @Transactional
    public ProductionOrder cancel(Long orderId) {
        // Step 1: Lock the order and validate status transition
        ProductionOrder order = productionOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (!order.getStatus().canMoveTo(OrderStatus.CANCELLED)) {
            throw new InvalidStatusException("Cannot move order " + orderId + " from " + order.getStatus() + " to " + OrderStatus.CANCELLED);
        }

        // Step 2: If currently RELEASED, release previously reserved stock and record UNRESERVE movements
        if (order.getStatus() == OrderStatus.RELEASED) {
            List<OrderRequirement> requirements = orderRequirementRepository.findByOrderId(orderId);
            List<Long> ingredientIds = requirements.stream()
                    .map(r -> r.getIngredient().getId())
                    .sorted()
                    .toList();

            List<Ingredient> lockedIngredients = ingredientRepository.findAllByIdForUpdate(ingredientIds);
            Map<Long, Ingredient> ingredientMap = lockedIngredients.stream()
                    .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

            for (OrderRequirement req : requirements) {
                Ingredient ing = ingredientMap.get(req.getIngredient().getId());
                BigDecimal required = req.getRequiredQty();

                ing.setReserved(ing.getReserved().subtract(required));

                StockMovement movement = new StockMovement(
                        ing,
                        order,
                        MovementType.UNRESERVE,
                        required,
                        ing.getOnHand(),      // on_hand_after
                        ing.getReserved()     // reserved_after
                );
                stockMovementRepository.save(movement);
            }

            // Explicit save: guarantees ingredient balance changes are flushed regardless of dirty-check.
            ingredientRepository.saveAll(lockedIngredients);
        }
        // If DRAFT: no stock was reserved, so no inventory updates or movements are recorded

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(OffsetDateTime.now());
        return order;
    }

    /**
     * 5) Read methods
     */
    @Transactional(readOnly = true)
    public ProductionOrder getOrder(Long id) {
        return productionOrderRepository.findByIdWithRecipe(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProductionOrder> listOrders(OrderStatus statusOrNull) {
        if (statusOrNull == null) {
            return productionOrderRepository.findAllByOrderByCreatedAtDesc();
        }
        return productionOrderRepository.findByStatusOrderByCreatedAtDesc(statusOrNull);
    }

    @Transactional(readOnly = true)
    public List<RequirementLine> getRequirementLines(ProductionOrder order) {
        if (order.getStatus() == OrderStatus.DRAFT) {
            // Live calculation from recipe per-batch quantity and order batches
            List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(order.getRecipe().getId());
            List<RequirementLine> lines = new ArrayList<>();
            for (RecipeIngredient ri : recipeIngredients) {
                Ingredient ing = ingredientRepository.findById(ri.getIngredient().getId())
                        .orElse(ri.getIngredient());
                BigDecimal required = ri.getQtyPerBatch().multiply(BigDecimal.valueOf(order.getBatches()));
                BigDecimal available = ing.getAvailable();
                BigDecimal shortfall = (available.compareTo(required) < 0)
                        ? required.subtract(available)
                        : BigDecimal.ZERO;
                lines.add(new RequirementLine(ing, required, available, shortfall));
            }
            return lines;
        } else {
            // Build from saved OrderRequirement snapshot
            List<OrderRequirement> requirements = orderRequirementRepository.findByOrderId(order.getId());
            List<RequirementLine> lines = new ArrayList<>();
            for (OrderRequirement req : requirements) {
                Ingredient ing = ingredientRepository.findById(req.getIngredient().getId())
                        .orElse(req.getIngredient());
                BigDecimal required = req.getRequiredQty();
                BigDecimal available = ing.getAvailable();
                BigDecimal shortfall = (available.compareTo(required) < 0)
                        ? required.subtract(available)
                        : BigDecimal.ZERO;
                lines.add(new RequirementLine(ing, required, available, shortfall));
            }
            return lines;
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsForOrder(Long orderId) {
        return stockMovementRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
}
