package com.lilithsthrone.game.sex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
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
import com.lilithsthrone.main.Main;
import com.lilithsthrone.testutil.GameBootstrap;
import com.lilithsthrone.testutil.JavaFxHarness;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * Golden-master safety net for the {@code SexPace} switch modernization in {@link SexAreaOrifice}
 * and {@link SexAreaPenetration}. It drives {@code getSexDescription(...)} across the full matrix of
 * (receiver area x target area x pastTense x performerPace x targetPace) with two distinct awake
 * characters (so the non-inanimate pace-switch branches are exercised), and hashes the concatenated
 * raw output.
 *
 * <p>The switch-statement -> switch-expression conversion must leave this hash byte-identical. The
 * expected hash is stored under {@code test/resources/snapshots/}; the first run bootstraps it, and
 * on mismatch the full actual output is dumped to {@code target/} for a diff.
 */
@ExtendWith(JavaFxHarness.class)
class SexAreaDescriptionSnapshotTest {

	private static final File SNAPSHOT = new File("test/resources/snapshots/sexAreaDescriptions.sha256");
	private static PlayerCharacter performer;
	private static PlayerCharacter target;

	@BeforeAll
	static void boot() {
		GameBootstrap.boot();
		// Create BOTH characters after a fixed seed so their generated bodies are deterministic
		// across JVM runs (the bootstrap player is generated pre-seed, so we don't use it here).
		Util.random = new Random(20240101);
		performer = new PlayerCharacter(new NameTriplet("Perf"), 1, null,
				Gender.M_P_MALE, Subspecies.HUMAN, RaceStage.HUMAN,
				WorldType.MUSEUM, PlaceType.MUSEUM_ENTRANCE);
		target = new PlayerCharacter(new NameTriplet("Target"), 1, null,
				Gender.F_V_B_FEMALE, Subspecies.HUMAN, RaceStage.HUMAN,
				WorldType.MUSEUM, PlaceType.MUSEUM_ENTRANCE);
	}

	private static String describe(SexAreaInterface area, SexAreaInterface targetArea, boolean pastTense,
			SexPace pp, SexPace tp) {
		Util.random = new Random(1234); // pin any seedable name randomness
		try {
			return area.getSexDescription(pastTense, performer, pp, target, tp, targetArea);
		} catch (Throwable t) {
			return "EX:" + t.getClass().getSimpleName();
		}
	}

	private static String buildSnapshot() {
		StringBuilder sb = new StringBuilder();
		SexAreaInterface[] areas = concat(SexAreaOrifice.values(), SexAreaPenetration.values());
		for (SexAreaInterface area : areas) {
			for (SexAreaInterface targetArea : areas) {
				for (boolean pastTense : new boolean[] { true, false }) {
					for (SexPace pp : SexPace.values()) {
						for (SexPace tp : SexPace.values()) {
							sb.append(area).append('|').append(targetArea).append('|').append(pastTense)
									.append('|').append(pp).append('|').append(tp).append("=>")
									.append(describe(area, targetArea, pastTense, pp, tp)).append('\n');
						}
					}
				}
			}
		}
		return sb.toString();
	}

	private static SexAreaInterface[] concat(SexAreaInterface[] a, SexAreaInterface[] b) {
		SexAreaInterface[] out = new SexAreaInterface[a.length + b.length];
		System.arraycopy(a, 0, out, 0, a.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	private static String sha256(String s) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (byte b : digest) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	@Test
	@DisplayName("getSexDescription output hash is stable across the SexPace switch conversion")
	void sexDescriptionOutputIsStable() throws Exception {
		String snapshot = buildSnapshot();
		String hash = sha256(snapshot);

		if (!SNAPSHOT.exists()) {
			SNAPSHOT.getParentFile().mkdirs();
			Files.writeString(SNAPSHOT.toPath(), hash, StandardCharsets.UTF_8);
			System.out.println("[SexAreaDescriptionSnapshotTest] baseline hash created: " + hash);
			return;
		}

		String expected = Files.readString(SNAPSHOT.toPath(), StandardCharsets.UTF_8).trim();
		if (!expected.equals(hash)) {
			File dump = new File("target/sexAreaDescriptions.actual.txt");
			Files.writeString(dump.toPath(), snapshot, StandardCharsets.UTF_8);
			System.err.println("[SexAreaDescriptionSnapshotTest] mismatch; full output dumped to " + dump.getAbsolutePath());
		}
		assertThat(hash).as("getSexDescription golden-master hash").isEqualTo(expected);
	}
}
