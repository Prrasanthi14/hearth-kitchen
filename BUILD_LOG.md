# Build Log

**Name:** V S Prrasanthi
**Repository URL:** TODO  
**Start (date and time):** THu 29 OCT 3pm 
**End (date and time):** 6.00pm 
**Total working time:** 3 hours + testing/debuggin scenarios

---

## 1. Timeline

| Start | End | What I worked on | Outcome | AI tool used? |
|---|---|---|---|---|
| TODO | TODO | Project setup, schema migration, seed data, models, and repositories | DB schema and entities created with Flyway migrations | Yes |
| TODO | TODO | Core business logic, locking strategy, DTOs, and Service layer | OrderService and StockService implemented | Yes |
| TODO | TODO | Controller layer, Thymeleaf templates, CSS, and PRG flows | Functional UI with zero JavaScript | Yes |
| TODO | TODO | Automated testing (unit, web, Testcontainers) and documentation | 24 passing tests, README, and Build Log | Yes |

---

## 2. Decisions and assumptions

| # | Question or decision point | Options I considered | What I chose | Why |
|---|---|---|---|---|
| 1 | Order status modeling | Full GoF State pattern / Enum with transition method | Enum state machine (`OrderStatus.canMoveTo`) | Simple, explicit, sufficient for 4 states, easy to explain without over-engineering |
| 2 | Unit of measurement storage | Float/Double / Mixed display units / Base units with display factor | Base units (`NUMERIC(14,3)`) + `display_factor` | Eliminates IEEE 754 floating-point drift, ensures zero rounding loss across unit conversions |
| 3 | Order cancellation scope | Allow from any status / Allow from DRAFT and RELEASED only | DRAFT and RELEASED only | Completed orders represent consumed physical inventory; cancelling terminal states corrupts physical reality |
| 4 | Movement naming clarity | Use "RELEASE" as unreserve movement / Rename to UNRESERVE | Renamed to `UNRESERVE` | Disambiguates order release (which reserves stock) from releasing previously held reservations |
| 5 | Recipe changes over time | Re-query recipe on completion / Snapshot requirements | Requirements snapshot (`order_requirement`) at release | Guarantees auditability and ensures changes to future recipe formulas do not alter in-flight or past orders |
| 6 | Concurrency & race condition control | Java `synchronized` / Optimistic `@Version` / Database row locks | Pessimistic write locks (`SELECT ... FOR UPDATE`) with sorted ingredient IDs | Works seamlessly across multiple horizontal app instances and prevents deadlocks by establishing a global acquisition order |
| 7 | Failed release behavior | Auto-cancel order / Keep order in DRAFT | Keep order in DRAFT | Allows kitchen manager to restock and retry the order without re-entering batch parameters |
| 8 | Frontend technology | SPA (React/Vue) / Thymeleaf with JS / Server-rendered Thymeleaf with zero JS | Server-rendered Thymeleaf with NO JavaScript | Meets strict constraint; relies on standard HTML forms and Post-Redirect-Get (PRG) pattern |
| 9 | Java LTS version | Java 21 / Java 17 | Java 17 | Matches local runtime environment and ensures broad enterprise compatibility |
| 10 | Restock request feature | Implement full restock entity / Focus on core production and stock ledger | Restock request skipped due to timebox | Prioritized correctness, deterministic locking, and thorough automated test coverage for core order flow |

---

## 3. Data model

| Table | Purpose | Key columns | Relationships |
|---|---|---|---|
| `ingredient` | Master catalog of raw materials and inventory balances | `id`, `name`, `base_unit`, `display_unit`, `display_factor`, `on_hand`, `reserved` | 1-to-many with `recipe_ingredient`, `order_requirement`, `stock_movement` |
| `recipe` | Master catalog of prepared bakery products and yields | `id`, `name`, `yield_qty`, `yield_unit` | 1-to-many with `recipe_ingredient`, `production_order` |
| `recipe_ingredient` | Bill of materials specifying base quantity required per batch | `id`, `recipe_id`, `ingredient_id`, `qty_per_batch` | FK to `recipe`, FK to `ingredient` (unique pair) |
| `production_order` | Lifecycle tracking of production runs | `id`, `recipe_id`, `batches`, `status`, `created_at`, `released_at`, `completed_at`, `cancelled_at` | FK to `recipe` |
| `order_requirement` | Immutable snapshot of reserved base quantities at release time | `id`, `order_id`, `ingredient_id`, `required_qty` | FK to `production_order`, FK to `ingredient` |
| `stock_movement` | Append-only ledger recording all inventory changes | `id`, `ingredient_id`, `order_id`, `movement_type`, `quantity`, `on_hand_after`, `reserved_after`, `created_at` | FK to `ingredient`, FK to `production_order` |

---

## 4. AI tool usage

**Tools used:** TODO  
**Raw logs:** in the `ai-logs/` folder  

| # | What I asked the AI | What it produced | What I kept, changed or rejected, and why |
|---|---|---|---|
| 1 | TODO | TODO | TODO |
| 2 | TODO | TODO | TODO |

**Parts I wrote or designed without AI:**
TODO

---

## 5. Problems and fixes

| Problem | How I found it | Fix | Time lost |
|---|---|---|---|
| Docker daemon socket / API 1.44 incompatibility | Testcontainers returned HTTP 400 Bad Request on newer Docker Desktop | Added `api.version=1.44` in `docker-java.properties` and Maven surefire plugin | TODO |
| Java 21 API usage in generated tests (`getFirst()`) | Compiler error during `./mvnw clean spring-boot:run` on Java 17 | Replaced with `.get(0)` for strict Java 17 backward compatibility | TODO |
| Port 5432 conflict with local Homebrew PostgreSQL service | Connection refused / role "hearth" does not exist error | Stopped host `postgresql@14` service using `brew services stop` to unbind port 5432 for Docker | TODO |

---

## 6. Testing

| # | Scenario | Steps | Expected result | Actual result | Pass / Fail |
|---|---|---|---|---|---|
| 1 | Release order with sufficient stock | Create Brioche x2, call `release()` | Stock reserved (Butter reserved 2400g, on-hand 8000g), 7 RESERVE movements written, status RELEASED | As expected | Pass |
| 2 | Release order with insufficient stock | Create Brioche x7, call `release()` | Throws `InsufficientStockException` listing both Butter (short 400g) and Eggs (short 6 pcs); zero stock reserved; status stays DRAFT | As expected | Pass |
| 3 | Prior reservation reduces availability | Release Brioche x4, then attempt another Brioche x4 | Second release fails with shortages on Butter (short 1600g) and Eggs (short 24 pcs) | As expected | Pass |
| 4 | Complete order consumes reserved stock | Create Brioche x2, release, then complete | Butter on-hand reduces to 5600g, reserved resets to 0g, 7 CONSUME movements recorded, status COMPLETED | As expected | Pass |
| 5 | Cancel released order frees reserved stock | Create Brioche x2, release, then cancel | Butter reserved resets to 0g, on-hand remains 8000g, 7 UNRESERVE movements recorded, status CANCELLED | As expected | Pass |
| 6 | Cancel draft order writes no movements | Create Brioche x1 in DRAFT, call `cancel()` | Status becomes CANCELLED, zero movements recorded in stock ledger | As expected | Pass |
| 7 | Invalid status transitions are rejected | Attempt to complete a DRAFT order; attempt to release a COMPLETED order | `InvalidStatusException` thrown in both cases | As expected | Pass |
| 8 | Concurrent order releases (pessimistic lock race) | Launch two simultaneous threads attempting to release Brioche x4 | Exactly one succeeds (RELEASED), one fails (DRAFT); Butter reserved is exactly 4800g; no overselling | As expected | Pass |

---

## 7. Unfinished work and known issues

- **Restock request:** In a full production system, when an order release fails due to shortage, an automated restock request should be created in the background.
- **Editing Draft batches:** Users currently cannot edit the number of batches on a Draft order; they must cancel and recreate it.
- **Recording actual usage different from reserved:** Kitchen actuals (e.g. spilling 100g flour or adjusting batch yield) currently assume exact recipe consumption on complete.
- **No authentication:** Currently open without role-based access control (e.g., kitchen manager vs kitchen staff).

---

## 8. Next steps

1. Restock request in a separate transaction (`REQUIRES_NEW`) with a unique `OPEN` request per ingredient so shortages automatically alert purchasing.
2. Stock receipt movement (`RECEIPT`) to allow kitchen staff to log inbound deliveries and replenish inventory through the UI.
3. Partial/actual consumption adjustments on order completion to account for kitchen waste and ingredient discrepancies.
4. Authentication and user attribution on each stock movement ledger record.
5. Containerize the application itself within `docker-compose.yml` for unified single-command deployment.

---

## 9. Time summary

| Phase | Minutes |
|---|---|
| Understanding the brief and design | TODO |
| Database and backend | TODO |
| Frontend | TODO |
| Testing | TODO |
| README and build log | TODO |
| **Total** | TODO |
