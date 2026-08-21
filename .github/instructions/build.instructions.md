---
applyTo: "{pom.xml,build.ps1,run.ps1,test.ps1,dev.ps1,mvnw,mvnw.cmd}"
---
# Build instructions — Lilith's Throne

## Non-negotiables
- Build & run on **JDK 17** (Temurin). JDK-17-only — JDK 8 support was dropped.
- `UtilText.java` imports `org.openjdk.nashorn.*` directly (committed). There is NO antrun
  import swap and NO worktree anymore — everything builds in-workspace. Don't reintroduce the
  `jdk.nashorn` import or the swap.
- Prefer the Maven Wrapper: `./mvnw` (or the thin `*.ps1` wrappers that pin `JAVA_HOME`).

## pom.xml facts
- Non-standard layout: `<sourceDirectory>src</sourceDirectory>`; test sources under `test/`
  via `<testSourceDirectory>`.
- `maven.compiler.release = 11` (bytecode target), toolchain is JDK 17.
- JavaFX (`org.openjfx`, platform classifier) and Nashorn (`org.openjdk.nashorn:nashorn-core`)
  come from Maven Central (profiles activated on JDK 11+).
- `res/` is copied into `target/Lilith's Throne (<platform>)/res` during `package`; it is large
  (~5,500 files) — the dev loop deliberately skips it.

## Performance guidance (Phase 2 — DONE; keep these invariants)
- `dev.ps1` is the fast inner loop: OFFLINE (`-o`) incremental `mvnw compiler:compile` + run from
  `target/classes` + `src` + deps on the classpath. Steady-state ~3s vs ~105s for a full build.
- Compiler is set to `<useIncrementalCompilation>false</useIncrementalCompilation>` — this is the
  FAST setting (per-file staleness). The default `true` recompiles ALL sources on any change.
- Run Maven OFFLINE for inner-loop compiles; remote metadata re-checks cost ~50s here.
- `UtilText.java` (~11k lines) takes ~55s to compile ALONE — editing it costs one ~1min compile
  (expected until the file is split). Editing anything else stays ~3s.
- `dev.ps1` skips process-resources (the big res/ copy) and shade. Internal resources (fxml/css/svg)
  are served from `src` on the classpath; external assets from the workspace's `res/` (the cwd).

## Validation after any build change
1. `./build.ps1` (clean) succeeds and `./run.ps1` launches the game.
2. `./test.ps1` is green (20 tests).
3. Manual smoke: new game → save → load → enter combat/sex → dialogue text renders.

## Distribution (standalone, no JDK required)
- `./package.ps1` builds a self-contained app-image with **jpackage**: `dist\Lilith's Throne\`
  with a native `Lilith's Throne.exe`, a bundled JRE (~93MB), the shaded jar, and `res/`.
  Flags: `-Rebuild` (clean-rebuild the jar first — use for a real release), `-Zip` (also emit a
  shareable `.zip`). The recipient does NOT need Java installed.
- Launch it from its own folder (double-click): the game reads `res/` and writes `data/` relative
  to the working directory, so the exe and `res/` must sit together.
- Bundled runtime modules = `jdeps --print-module-deps` on the shaded jar, PLUS `java.xml` (JAXP),
  `java.datatransfer` (clipboard) and `jdk.localedata`/`jdk.charsets` (used reflectively, invisible to jdeps).
- `dist/` is gitignored.
- **macOS / Linux:** jpackage cannot cross-compile. Run the same jpackage invocation ON that OS
  (same `--add-modules`, same shaded jar). It yields a `.app`/`.dmg` on macOS and a directory or
  `.deb`/`.rpm` on Linux. JavaFX ships inside the shaded jar, so only a JDK 17 with jpackage is needed there.
