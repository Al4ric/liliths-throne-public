# Copilot / AI agent steering instructions — Lilith's Throne

These instructions are always-on guidance for AI coding agents working in this repo.
They encode hard rules, the build/run workflow, and gotchas that are expensive to
rediscover. Read `docs/ARCHITECTURE.md` for the system overview.

## What this project is
A large single-module JavaFX desktop game (~980 `.java` files, ~5,500 `res/` asset
files). Game text/content lives in `res/**/*.xml` and uses an embedded scripting
language (`#IF/#ELSEIF/#ELSE` + inline JavaScript) evaluated at runtime by Nashorn.

- Build system: Maven (`pom.xml`) via the **Maven Wrapper** (`./mvnw`), non-standard layout — `<sourceDirectory>src</sourceDirectory>` (no `src/main/java`).
- Toolchain: build & run on **JDK 17** (Temurin); JDK-17-only (JDK 8 support was dropped). Source level is `release 11`.
- Entry points: `com.lilithsthrone.Launcher` (jar `Main-Class`) → `com.lilithsthrone.main.Main` (JavaFX `Application`).

## HARD RULES (do not violate)
1. **Tests before architecture/behavioral changes.** This is a large, runtime-heavy Java
   game: *"it compiles" proves nothing.* Before changing any behavior, ensure a
   characterization test pins the current behavior, then keep it green.
2. **Do NOT refactor randomness right now.** `Math.random()` is used in 200+ places and is
   unseedable. Deterministic full-playthrough tests are out of scope until explicitly
   planned. Lean on save/load fidelity + invariants + the seedable `Util.random` subset.
3. **Content scripting is Nashorn.** `UtilText.java` imports `org.openjdk.nashorn.*` directly
   (committed; the old JDK-8 `jdk.nashorn` import + antrun swap were removed when we went
   JDK-17-only). The `org.openjdk.nashorn:nashorn-core` dependency provides it. Don't reintroduce
   the `jdk.nashorn` import.
4. Keep changes minimal and scoped. No drive-by refactors, no gameplay rebalancing.

## Build & run workflow
Everything runs **in-workspace** via the Maven Wrapper (`./mvnw`) — no worktree, no import
swapping. The thin PowerShell scripts just pin `JAVA_HOME` to JDK 17 and wrap `mvnw`.

- `./build.ps1` — `mvnw clean package`: the shaded release JAR into `target/Lilith's Throne (win)/`.
- `./run.ps1` — launch the most recently built JAR with JDK 17.
- `./dev.ps1` — FAST inner loop (~3s): incremental offline `compiler:compile` + run straight from `target/classes` (no shade, no res copy). Day-to-day loop.
- `./test.ps1` — `mvnw test` (the JUnit suite).
- `./package.ps1` — self-contained distributable via jpackage (`dist/Lilith's Throne/` with a native `.exe` + bundled JRE + res; no JDK needed to run). `-Rebuild` for a fresh jar, `-Zip` for a shareable zip.

Plain `./mvnw test` / `./mvnw package` / `./mvnw javafx:run` also work directly (just ensure
`JAVA_HOME` points at JDK 17). Use `dev.ps1` while iterating; `build.ps1` produces the shippable JAR.

## Resolved: the old Nashorn/worktree gotcha
- Historically `UtilText.java` shipped a JDK-8 `jdk.nashorn` import, an antrun plugin swapped it
  to `org.openjdk.nashorn` at build time, and VS Code's JDT wrote a broken "poison" class on
  JDK 17 — which is why builds used an isolated worktree. **All of that is gone.** The import is
  committed as `org.openjdk.nashorn`, antrun is removed, and builds/tests run in the workspace.
  A stale `%USERPROFILE%\lt-build` worktree from the old flow can be removed with
  `git worktree remove` if present.

## Testing conventions
- Framework: JUnit 5 + AssertJ. Minimal mocking (only for genuinely complex/branching logic).
- Priority order (most valuable first): save/load round-trip fidelity → new-game invariants
  → dialogue/text (`UtilText.parse`) snapshots → pure-logic units.
- Tests must be **durable**: assert observable input→output, not implementation details.
- Test sources live in `test/` (configured via `<testSourceDirectory>` in `pom.xml`), kept
  out of the shaded jar.
- Some tests need the JavaFX toolkit booted once — use the shared harness in
  `test/com/lilithsthrone/testutil/`.
- See `.github/instructions/testing.instructions.md` for details.

## Known hotspots (see docs/ARCHITECTURE.md for depth)
- Startup: parallel world-gen image I/O (`world/Generation.java`), Nashorn init, ~600 static
  content initializers (`ItemType`/`ClothingType`/`Subspecies`).
- Save/load: XML DOM in `game/Game.java` (`exportGame`/`importGame`); an O(n³) loop and many
  `getElementsByTagName` rescans in the import path.
- Memory: static unbounded `Game.informationTooltips`; unremoved listeners in
  `controller/FileController.java`; unbounded SVG colour caches in `rendering/SVGImages.java`.
