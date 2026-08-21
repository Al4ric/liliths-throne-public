package com.lilithsthrone.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link Main#isVersionOlderThan(String, String)}.
 * Pure logic, no game state or JavaFX toolkit required. Pins current behavior
 * of the save-version comparison used throughout the load path.
 */
class VersionComparisonTest {

	@Test
	@DisplayName("an earlier patch is older than a later patch")
	void earlierPatchIsOlder() {
		assertThat(Main.isVersionOlderThan("0.4.11.2", "0.4.11.3")).isTrue();
	}

	@Test
	@DisplayName("equal versions are not older than each other")
	void equalVersionsAreNotOlder() {
		assertThat(Main.isVersionOlderThan("0.4.11.3", "0.4.11.3")).isFalse();
	}

	@Test
	@DisplayName("a later patch is not older than an earlier patch")
	void laterPatchIsNotOlder() {
		assertThat(Main.isVersionOlderThan("0.4.11.3", "0.4.11.2")).isFalse();
	}

	@Test
	@DisplayName("numeric (not lexical) comparison: 10 is newer than 9")
	void numericComparisonBeatsLexical() {
		assertThat(Main.isVersionOlderThan("0.4.9.1", "0.4.10.1")).isTrue();
		assertThat(Main.isVersionOlderThan("0.4.10.1", "0.4.9.1")).isFalse();
	}

	@Test
	@DisplayName("missing trailing components are treated as zero")
	void missingComponentsAreZero() {
		assertThat(Main.isVersionOlderThan("0.4.2", "0.4.2.1")).isTrue();
		assertThat(Main.isVersionOlderThan("0.4.2.1", "0.4.2")).isFalse();
	}
}
