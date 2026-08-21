package com.lilithsthrone.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
	 * The durable goal for "improve loading save": load a save, re-save it, assert no drift.
	 * Blocked until a headless load path exists — {@code importGame} requires the JavaFX
	 * {@code MainController} (via {@code setContent}/{@code endTurn}). Enable once Phase 4 extracts
	 * DOM→state loading from the UI tail, then assert idempotence of cycle 2 vs cycle 3.
	 */
	@Test
	@Disabled("Blocked: importGame is UI-coupled (endTurn -> MainController.updateUI; setContent). Enable after Phase 4 headless load path.")
	@DisplayName("save -> load -> save is idempotent (full round-trip)")
	void roundTripThroughImportIsStable() throws Exception {
		Game.exportGame("test_roundtrip_0", true, true);
		Game.importGame("test_roundtrip_0");
		Game.exportGame("test_roundtrip_1", true, true);

		Game.importGame("test_roundtrip_1");
		Game.exportGame("test_roundtrip_2", true, true);

		assertThat(readSave("test_roundtrip_2")).isEqualTo(readSave("test_roundtrip_1"));
	}
}
