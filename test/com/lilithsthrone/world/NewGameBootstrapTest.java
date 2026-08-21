package com.lilithsthrone.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.lilithsthrone.game.Game;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.testutil.GameBootstrap;
import com.lilithsthrone.testutil.JavaFxHarness;
import com.lilithsthrone.world.places.PlaceType;

/**
 * Characterization / invariant tests for the headless new-game boot: world generation and
 * player placement. RNG-agnostic — asserts structural invariants, never exact values.
 * These pin that the (expensive) startup path completes without exceptions and produces a
 * coherent world, so later startup optimizations can be verified against them.
 */
@ExtendWith(JavaFxHarness.class)
class NewGameBootstrapTest {

	@BeforeAll
	static void boot() {
		GameBootstrap.boot();
	}

	@Test
	@DisplayName("world generation populates the worlds map")
	void worldsAreGenerated() {
		Map<AbstractWorldType, World> worlds = Main.game.getWorlds();
		assertThat(worlds).isNotEmpty();
		assertThat(WorldType.getAllWorldTypes()).isNotEmpty();
	}

	@Test
	@DisplayName("the starting world (Museum) exists with a non-empty cell grid")
	void startingWorldHasGrid() {
		World museum = Main.game.getWorlds().get(WorldType.MUSEUM);
		assertThat(museum).as("Museum world").isNotNull();
		assertThat(museum.getCellGrid()).as("Museum grid columns").isNotEmpty();
		assertThat(museum.getCellGrid()[0]).as("Museum grid rows").isNotEmpty();
	}

	@Test
	@DisplayName("every successfully generated world has a populated cell grid")
	void generatedWorldsHaveGrids() {
		for (Map.Entry<AbstractWorldType, World> entry : Main.game.getWorlds().entrySet()) {
			World world = entry.getValue();
			if (world != null) {
				assertThat(world.getCellGrid())
						.as("cell grid for world %s", String.valueOf(entry.getKey()))
						.isNotEmpty();
			}
		}
	}

	@Test
	@DisplayName("the player is created and placed in the active (Museum) world")
	void playerIsPlacedInActiveWorld() {
		assertThat(Main.game.getPlayer()).isNotNull();

		World active = Main.game.getActiveWorld();
		assertThat(active).as("active world resolved from player location").isNotNull();
		assertThat(active.getWorldType()).isEqualTo(WorldType.MUSEUM);

		assertThat(active.getCell(PlaceType.MUSEUM_ENTRANCE))
				.as("Museum entrance cell").isNotNull();
	}

	@Test
	@DisplayName("the singleton game is wired up")
	void gameSingletonIsSet() {
		assertThat(Main.game).isInstanceOf(Game.class);
		assertThat(Main.sex).isNotNull();
		assertThat(Main.combat).isNotNull();
	}
}
