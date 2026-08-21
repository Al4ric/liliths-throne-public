package com.lilithsthrone.testutil;

import java.io.File;
import java.lang.reflect.Field;

import com.lilithsthrone.game.Game;
import com.lilithsthrone.game.Properties;
import com.lilithsthrone.game.character.PlayerCharacter;
import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.character.persona.NameTriplet;
import com.lilithsthrone.game.character.race.RaceStage;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.game.combat.Combat;
import com.lilithsthrone.game.sex.Sex;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.world.Generation;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * Boots the core game state for a fresh game HEADLESSLY (no JavaFX Scene / no
 * {@code MainController}). Mirrors the non-UI parts of
 * {@link Main#startNewGame(com.lilithsthrone.game.dialogue.DialogueNode)}:
 * init statics → {@code new Game()} → generate worlds synchronously → create the player.
 *
 * <p>Deliberately stops short of {@code initNewGame()} / {@code setContent()} because
 * those dereference the (null) {@code Main.mainController}. This bootstrap exercises the
 * real, expensive startup path — world generation and player placement — with zero mocking.
 *
 * <p>Idempotent: the first call boots; subsequent calls are no-ops so the (slow) world
 * generation is shared across tests in a JVM.
 */
public final class GameBootstrap {

	private static boolean booted = false;

	private GameBootstrap() {
	}

	public static synchronized void boot() {
		if (booted) {
			return;
		}
		new File("data").mkdirs();
		new File("data/saves").mkdirs();
		new File("data/characters").mkdirs();

		if (Main.getProperties() == null) {
			setProperties(new Properties());
		}

		// Game must exist before Sex/Combat: their static initializers (SexActionManager ->
		// GenericOrgasms -> SexAction) dereference Main.game.isInSex() during class-load.
		Main.game = new Game();
		Main.sex = new Sex();
		Main.combat = new Combat();

		// Generation extends javafx.concurrent.Task; call() is pure logic and safe synchronously.
		new Generation().call();

		Main.game.setPlayer(new PlayerCharacter(
				new NameTriplet("Player"), 1, null,
				Gender.M_P_MALE, Subspecies.HUMAN, RaceStage.HUMAN,
				WorldType.MUSEUM, PlaceType.MUSEUM_ENTRANCE));

		booted = true;
	}

	// Main.properties is a private static field with no setter; set it directly for tests.
	private static void setProperties(Properties p) {
		try {
			Field f = Main.class.getDeclaredField("properties");
			f.setAccessible(true);
			f.set(null, p);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to seed Main.properties for tests", e);
		}
	}
}
