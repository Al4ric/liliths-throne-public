package com.lilithsthrone.game.sex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.lilithsthrone.game.character.PlayerCharacter;
import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.character.persona.NameTriplet;
import com.lilithsthrone.game.character.race.RaceStage;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.testutil.GameBootstrap;
import com.lilithsthrone.testutil.JavaFxHarness;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * Durable characterization of {@link SexAreaOrifice} / {@link SexAreaPenetration}
 * {@code getSexDescription(...)}. It drives the full matrix of
 * (receiver area x target area x pastTense x performerPace x targetPace) with two distinct awake
 * characters (so the non-inanimate pace-switch branches are exercised) and asserts observable
 * invariants that hold regardless of RNG or JavaFX/JDK version: no combination throws, the parsed
 * output never leaks an unresolved parser tag, and never emits a stray {@code null}.
 *
 * <p>This guards the {@code SexPace} switch modernization (and any future edit) without pinning the
 * exact random prose, which legitimately varies across dependency/JDK changes.
 */
@ExtendWith(JavaFxHarness.class)
class SexAreaDescriptionSnapshotTest {

	private static PlayerCharacter performer;
	private static PlayerCharacter target;

	@BeforeAll
	static void boot() {
		GameBootstrap.boot();
		Util.random = new Random(20240101);
		performer = new PlayerCharacter(new NameTriplet("Perf"), 1, null,
				Gender.M_P_MALE, Subspecies.HUMAN, RaceStage.HUMAN,
				WorldType.MUSEUM, PlaceType.MUSEUM_ENTRANCE);
		target = new PlayerCharacter(new NameTriplet("Target"), 1, null,
				Gender.F_V_B_FEMALE, Subspecies.HUMAN, RaceStage.HUMAN,
				WorldType.MUSEUM, PlaceType.MUSEUM_ENTRANCE);
	}

	private static SexAreaInterface[] allAreas() {
		SexAreaOrifice[] a = SexAreaOrifice.values();
		SexAreaPenetration[] b = SexAreaPenetration.values();
		SexAreaInterface[] out = new SexAreaInterface[a.length + b.length];
		System.arraycopy(a, 0, out, 0, a.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	@Test
	@DisplayName("getSexDescription never throws, leaks a parser tag, or emits null across the full matrix")
	void sexDescriptionMatrixIsWellFormed() {
		List<String> violations = new ArrayList<>();
		SexAreaInterface[] areas = allAreas();
		for (SexAreaInterface area : areas) {
			for (SexAreaInterface targetArea : areas) {
				for (boolean pastTense : new boolean[] { true, false }) {
					for (SexPace pp : SexPace.values()) {
						for (SexPace tp : SexPace.values()) {
							String key = area + "|" + targetArea + "|" + pastTense + "|" + pp + "|" + tp;
							Util.random = new Random(1234);
							String out;
							try {
								out = area.getSexDescription(pastTense, performer, pp, target, tp, targetArea);
							} catch (Throwable t) {
								violations.add(key + " => THREW " + t.getClass().getSimpleName());
								continue;
							}
							if (out == null) {
								violations.add(key + " => null return");
							} else if (out.contains("[npc") || out.contains("[pc") || out.contains("[style")) {
								violations.add(key + " => leaked parser tag: " + snippet(out));
							} else if (out.contains("nullnull") || out.contains(" null ")) {
								violations.add(key + " => stray null: " + snippet(out));
							}
						}
					}
				}
			}
		}
		assertThat(violations).as("well-formedness violations").isEmpty();
	}

	private static String snippet(String s) {
		return s.length() <= 160 ? s : s.substring(0, 160);
	}
}
