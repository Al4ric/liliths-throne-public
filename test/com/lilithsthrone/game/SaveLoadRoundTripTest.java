package com.lilithsthrone.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import com.lilithsthrone.testutil.GameBootstrap;
import com.lilithsthrone.testutil.JavaFxHarness;

/**
 * Save/load fidelity golden master.
 *
 * <p>Scope note: {@code Game.importGame(...)} cannot yet run headlessly — its tail calls
 * {@code setContent(...)} and {@code endTurn(0)}, and {@code endTurn} unconditionally drives the
 * UI (static {@code MainController.updateUI()} + {@code Main.mainController.getTooltip().hide()}).
 * So the load path is entangled with the JavaFX controller. Until a headless load path exists
 * (planned Phase 4 — "improve loading save"), we pin the deterministic, RNG-free SAVE side:
 * serialization is stable and produces well-formed, structurally-complete XML. The full
 * import→export→import round-trip is captured as a {@link Disabled} test documenting the blocker.
 */
@ExtendWith(JavaFxHarness.class)
class SaveLoadRoundTripTest {

	@BeforeAll
	static void boot() {
		GameBootstrap.boot();
	}

	private static String readSave(String name) throws Exception {
		return Files.readString(new File("data/saves/" + name + ".xml").toPath(), StandardCharsets.UTF_8);
	}

	// Canonical, order-independent view of the save's state lines. Two things are normalized out:
	//  - the growing "Game loaded" event-log entries (a log, not core state);
	//  - line ORDER: NPCs are loaded via a parallelStream into a ConcurrentHashMap, so their save
	//    order varies between loads even though the data is identical. Sorting compares the data itself.
	private static String canonicalState(String save) {
		return java.util.Arrays.stream(save.split("\n"))
				.filter(line -> !line.contains("Game loaded"))
				.sorted()
				.collect(java.util.stream.Collectors.joining("\n"));
	}

	@Test
	@DisplayName("exporting the same state twice is byte-identical (serialization is deterministic)")
	void exportIsDeterministic() throws Exception {
		Game.exportGame("test_export_a", true, true);
		Game.exportGame("test_export_b", true, true);

		assertThat(readSave("test_export_a")).isEqualTo(readSave("test_export_b"));
	}

	@Test
	@DisplayName("an exported save is well-formed XML with the expected top-level structure")
	void exportedSaveIsWellFormedXml() throws Exception {
		Game.exportGame("test_export_structure", true, true);

		File saveFile = new File("data/saves/test_export_structure.xml");
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(saveFile);

		assertThat(doc.getElementsByTagName("game").getLength()).as("game root").isEqualTo(1);
		assertThat(doc.getElementsByTagName("coreInfo").getLength()).as("coreInfo").isEqualTo(1);
		assertThat(doc.getElementsByTagName("playerCharacter").getLength()).as("playerCharacter").isEqualTo(1);
		assertThat(doc.getElementsByTagName("maps").getLength()).as("maps").isEqualTo(1);
		assertThat(readSave("test_export_structure")).contains("Player");
	}

	/**
	 * "Improve loading save" fidelity guard: repeatedly load a save and re-save it via the headless
	 * load path {@code importGame(file, false)} (skips the UI-coupled setContent/endTurn tail). The
	 * core game state (ignoring the growing "Game loaded" event log) must reach an idempotent
	 * fixpoint — i.e. once normalized, load→save produces no drift. Non-convergence would mean the
	 * load path is lossy or keeps mutating state, which is exactly what this pins against.
	 */
	@Test
	@DisplayName("save -> load -> save converges to an idempotent fixpoint (headless load)")
	void roundTripThroughImportConverges() throws Exception {
		Game.exportGame("test_rt_0", true, true);
		String current = "test_rt_0";
		String previousState = null;

		for (int cycle = 1; cycle <= 6; cycle++) {
			Game.importGame(new File("data/saves/" + current + ".xml"), false);
			String next = "test_rt_" + cycle;
			Game.exportGame(next, true, true);

			String state = canonicalState(readSave(next));
			if (state.equals(previousState)) {
				return; // reached a fixpoint: load->save preserves state (order-independent)
			}
			previousState = state;
			current = next;
		}
		org.assertj.core.api.Assertions.fail("save/load did not reach an idempotent fixpoint within 6 cycles");
	}
}
