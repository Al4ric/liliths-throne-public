package com.lilithsthrone.game.character.body;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lilithsthrone.game.character.body.types.FluidType;
import com.lilithsthrone.game.character.body.valueEnums.FluidFlavour;
import com.lilithsthrone.game.character.body.valueEnums.FluidModifier;
import com.lilithsthrone.game.character.body.valueEnums.FluidTypeBase;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.testutil.GameBootstrap;
import com.lilithsthrone.testutil.JavaFxHarness;

/**
 * Characterisation tests for the urine fluid system (bladder storage, regeneration and
 * fluid serialisation), pinning the observable behaviour that mirrors the milk/cum systems.
 */
@ExtendWith(JavaFxHarness.class)
class UrineFluidTest {

	@BeforeAll
	static void boot() {
		GameBootstrap.boot();
	}

	@Test
	@DisplayName("a character with a penis is able to urinate and starts with a full, non-zero bladder")
	void bladderStartsFull() {
		var player = Main.game.getPlayer();

		assertThat(player.isAbleToUrinate()).isTrue();
		assertThat(player.getBladderRawUrineStorageValue()).isEqualTo(Bladder.DEFAULT_URINE_STORAGE);
		assertThat(player.getBladderRawStoredUrineValue()).isEqualTo(Bladder.DEFAULT_URINE_STORAGE);
		assertThat(player.isBladderFull()).isTrue();
		assertThat(player.getUrineType().getBaseType()).isEqualTo(FluidTypeBase.URINE);
	}

	@Test
	@DisplayName("voiding empties the bladder and filling tops it back up to the storage maximum")
	void voidAndFill() {
		var player = Main.game.getPlayer();

		player.voidBladder();
		assertThat(player.getBladderRawStoredUrineValue()).isEqualTo(0);
		assertThat(player.isBladderFull()).isFalse();

		player.fillBladderToMaxStorage();
		assertThat(player.getBladderRawStoredUrineValue()).isEqualTo(player.getBladderRawUrineStorageValue());
	}

	@Test
	@DisplayName("stored urine is clamped to the bladder's storage capacity")
	void storedUrineClampedToCapacity() {
		var player = Main.game.getPlayer();

		player.setUrineStorage(400);
		player.fillBladderToMaxStorage();
		assertThat(player.getBladderRawStoredUrineValue()).isEqualTo(400);

		// Shrinking the bladder must reduce the stored contents to fit.
		player.setUrineStorage(100);
		assertThat(player.getBladderRawStoredUrineValue()).isLessThanOrEqualTo(100);

		player.setUrineStorage(Bladder.DEFAULT_URINE_STORAGE);
		player.fillBladderToMaxStorage();
	}

	@Test
	@DisplayName("urine regenerates over time at the configured per-second rate")
	void regenerationPerSecond() {
		var player = Main.game.getPlayer();

		player.setUrineStorage(Bladder.DEFAULT_URINE_STORAGE);
		player.setUrineRegeneration(86_400); // 1 ml/second
		assertThat(player.getUrineRegenerationPerSecond()).isEqualTo(1f);

		player.voidBladder();
		player.incrementStoredUrine(player.getUrineRegenerationPerSecond() * 10);
		assertThat(player.getBladderRawStoredUrineValue()).isEqualTo(10f);

		player.fillBladderToMaxStorage();
	}

	@Test
	@DisplayName("FluidUrine survives an XML save/load round-trip (type, flavour and modifiers)")
	void fluidUrineRoundTrips() throws Exception {
		FluidUrine original = new FluidUrine(FluidType.URINE_HUMAN);
		original.setFlavour(null, FluidFlavour.STRAWBERRY);
		original.addFluidModifier(null, FluidModifier.ADDICTIVE);
		original.addFluidModifier(null, FluidModifier.BUBBLING);

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element parent = doc.createElement("test");
		doc.appendChild(parent);
		original.saveAsXML("urine", parent, doc);

		FluidUrine loaded = FluidUrine.loadFromXML("urine", parent, doc);

		assertThat(loaded.getType()).isEqualTo(original.getType());
		assertThat(loaded.getFlavour()).isEqualTo(FluidFlavour.STRAWBERRY);
		assertThat(loaded.getFluidModifiers()).containsExactlyInAnyOrder(FluidModifier.ADDICTIVE, FluidModifier.BUBBLING);
		assertThat(loaded).isEqualTo(original);
	}

	@Test
	@DisplayName("copying a body preserves the bladder's contents, capacity, regeneration and urine type")
	void bodyCopyPreservesBladder() {
		var player = Main.game.getPlayer();
		player.setUrineStorage(750);
		player.setUrineRegeneration(1234);
		player.fillBladderToMaxStorage();

		Body copy = new Body(player.getBody());

		assertThat(copy.getBladder().getRawUrineStorageValue()).isEqualTo(player.getBladderRawUrineStorageValue());
		assertThat(copy.getBladder().getRawStoredUrineValue()).isEqualTo(player.getBladderRawStoredUrineValue());
		assertThat(copy.getBladder().getRawUrineRegenerationValue()).isEqualTo(player.getRawUrineRegenerationValue());
		assertThat(copy.getBladder().getType()).isEqualTo(player.getUrineType());

		player.setUrineStorage(Bladder.DEFAULT_URINE_STORAGE);
	}
}
