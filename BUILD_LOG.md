# Build Log

**Name:** V S Prrasanthi
**Repository URL:** TODO  
**Start (date and time):** THu 29 OCT 3pm 
**End (date and time):** 6.00pm 
**Total working time:** 3 hours + testing/debugging scenarios

---

## 1. Timeline

| Start  | End     | What I worked on                                                                   | Outcome                                                                                                                                         | AI tool used? |
|--------|---------|------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| 3:00pm | 3:20 pm | Understanding the requirements and scenarios                                       | Understood the actual requirement, analysed how I can apply the locks, handle concurrency and basic apis needed                                 | NO            |
| 3.20pm | 4:00 pm | Project setup, schema migration, seed data, project structure creations with files | Project file structure creation initialization, docker set up ,ensuring all the basic files are created and boot run the project without errors | Yes           |
| 4:00pm | 5:30 pm | MVC layers, Thymeleaf templates and program flows and service logic creations      | Basic Crud operations were working and service logics were added                                                                                | Yes           |
| 5.40pm | 6:10 pm | Testing the flows for different scenarios                                          | Bugs were spotted and removed during testing the basic workflows are working                                                                    | NO            |

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

**Tools used:** Claude, Antigravity IDE  
**Raw logs:** in the `ai-logs/` folder  

| # | What I asked the AI                                                                                                                                                                 | What it produced                                                                                                                                 | What I kept, changed or rejected, and why                                                                                                                                          |
|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Claude - cross checking my understanding of requirements, ensuring the if I could apply any design patterns, how I can ensure the ACID property is properly managed in this project | gave me the project overall structure, and corrected me in case of any assumptions/deviations from requirement                                   |                                                                                                                                                                                    |
| 2 | Antigravity IDE- actual project structure implementation, logic correction                                                                                                          | Although it gave the basiclogic, during debuggin I caught lots of bugs like updating the tables were not done, it was just plain CRUD operations | I kept the basic code generated but asked it to modify the service logics especially the orders, status changes and updations in the table which were not correct from antigravity |

**Parts I wrote or designed without AI:**
- Spotted the bugs in the code generated by antigravity and fixed them, especially the service logics 
- Updation of the enums and tables when status changes happened - the code generated by antigravity was just plain CRUD operations, but I added the logic to update the tables when status changes happened.
- Status changes and the corresponding stock movements were not handled correctly in the generated code, so I added the logic to handle them correctly.
- Added the pessimistic locking to avoid race conditions and overselling of stock, and wrote the README and build log.
- Updated the timings displayed on the UI to be more human-readable instead of DB timings

---

## 5. Problems and fixes

Ordered by impact (most serious first).

| Problem | How I found it | Fix | Time lost |
|---|---|---|---|
| Completing an order didn't update stock in the database (on hand/reserved unchanged after consume) | Manual test: completed an order, then checked the stock page and `select * from ingredient` | The changes to the ingredient objects were not being saved. Made sure `complete()` runs inside `@Transactional`, works on the locked, managed entities loaded in the same transaction, and saves the CONSUME movements. Verified on hand and reserved both drop by the consumed amount | ~15 min |
| Thymeleaf form data not reaching the backend (fields arrived empty/null) | Manual UI test: submitted the create-order form, but the controller received no values | Form input `name`s didn't match the form object's fields. Bound the form with `th:object="${form}"` and `th:field="*{recipeId}"` / `*{batches}`, and used `@Valid @ModelAttribute` in the controller | ~10 min |
| "Resource Not Found" page when creating an order on `/orders` | Manual UI test: Create Brioche x1 | TODO: add the cause Antigravity found (e.g. form field name or recipe id didn't match `CreateOrderForm`). Fixed and added logging to the Not Found handler | ~15 min |
| Backend data not showing on the pages (empty tables or values) | Manual UI test: pages rendered but lists and values were blank | The template used attribute names different from what the controller added to the `Model`. Aligned the names (e.g. `model.addAttribute("orders", ...)` with `th:each="o : ${orders}"`) | ~10 min |
| Timestamps displayed in UTC instead of IST (e.g. 12:42 instead of 18:12) | Manual check of the orders list and movement history | Kept storing UTC in the database. Added one shared formatter (`@Component("dt")`) converting to Asia/Kolkata and used it in every template | ~10 min |
| App wouldn't start: `UnsupportedClassVersionError` (class file version 65.0 vs 61.0) | `spring-boot:run` failed at startup | Code was compiled with Java 21 but my local runtime is Java 17. Changed `java.version` to 17 in `pom.xml` and did a clean build | ~5 min |
| `release version 21 not supported` | Compilation failed after the Java version change | Java 21 was still set in another place in `pom.xml`. Set every Java version entry to 17. Also found that Homebrew `mvn` used a different, newer JDK than my Java 17, so switched to the Maven wrapper `./mvnw` only | ~5 min |
| Test compilation failed: `cannot find symbol getFirst()` | `testCompile` errors in the generated test class | AI-generated tests used `List.getFirst()`, a Java 21-only method. Replaced it with `get(0)` and added a "Java 17 only" constraint to later AI prompts | ~3 min |
| `Port 8080 already in use` when starting the app from IntelliJ | IntelliJ run failed after moving the app out of Antigravity | The app was still running as a background process started by Antigravity. Found it with `lsof -i :8080` and stopped it with `kill <PID>` | ~5 min |
| Docker commands failed: could not connect to the Docker API (`docker.sock` not found) | `docker ps` returned a connection error | The Docker daemon wasn't running. Started Docker Desktop (`open -a Docker`) and re-ran `docker ps` | ~5 min |
| `git push` failed: password authentication not supported | GitHub rejected my account password over HTTPS | GitHub requires a token. Installed the GitHub CLI (`brew install gh`), ran `gh auth login`, then `gh auth setup-git` | ~10 min |
| `git push` failed: `'origin' does not appear to be a git repository` | Pushing the branch to GitHub | No remote was configured. Created the GitHub repository and ran `git remote add origin <url>` | ~3 min |
| `Unknown lifecycle phase "build"` | Maven error when running `mvn clean build` | Maven has no `build` phase. Used `./mvnw clean spring-boot:run` (or `package` to build only) | ~2 min |

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
|---|---------|
| Understanding the brief and design | 20  |
| Database and backend               | 50  |
| Frontend                           | 15  |
| Testing                            | 70  |
| README and build log               | 25  |
| **Total**                          | 180 |
