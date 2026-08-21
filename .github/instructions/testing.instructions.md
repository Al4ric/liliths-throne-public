---
applyTo: "test/**/*.java"
---
# Testing instructions — Lilith's Throne

## Goals
- Durable, behavior-focused tests (input→output), not implementation-coupled.
- Prefer end-to-end / integration coverage with as little mocking as possible.
- Unit tests only for genuinely complex or branch-heavy pure logic.

## Framework & layout
- JUnit 5 (`org.junit.jupiter`) + AssertJ (`org.assertj`).
- Test sources live under `test/` (repo root), configured by `<testSourceDirectory>` in `pom.xml`.
- Package tests to mirror the class under test (`com.lilithsthrone.<area>`); shared helpers in
  `com.lilithsthrone.testutil`.
- Run with `./test.ps1` (in-workspace via `./mvnw`, JDK 25). No worktree/antrun rewrite anymore
  (the `org.openjdk.nashorn` import is committed).

## The JavaFX toolkit
- Some logic transitively touches JavaFX types and needs the toolkit booted once per JVM.
- Use `@ExtendWith(JavaFxHarness.class)` (in `com.lilithsthrone.testutil`) — it calls
  `Platform.startup(...)` once and is a no-op if already started.
- Do NOT create Scenes/Stages/`Image`s in tests unless the test specifically targets rendering.

## Determinism
- Do NOT try to make `Math.random()` deterministic (out of scope).
- For logic that uses `Util.random`, seed it in the test (`Util.random = new Random(SEED)`).
- Otherwise write **invariant/property** assertions that hold regardless of RNG
  (non-null, ranges, counts, structural equality), not exact-value assertions.

## Test tiers (write in this priority order)
1. **Save/load fidelity (golden master).** Import a save → export → re-import; assert
   structural equality / snapshot the exported XML. The load path is RNG-free → deterministic.
2. **New-game invariants.** Boot a new game headlessly; assert player exists, every world
   generated with a non-empty cell grid, unique NPCs spawned, no exceptions/nulls.
3. **Dialogue/text characterization.** Feed fixed game state to `UtilText.parse(...)` on
   representative snippets (including `#IF`/inline JS) and snapshot the HTML output. This is the
   guard for any future Nashorn change.
4. **Pure-logic units.** Version comparison, attribute/damage formulas, XML parsing helpers.

## Snapshots
- Store expected outputs under `test/resources/snapshots/`. On intentional behavior change,
  review the diff before updating the snapshot — never blind-update.
