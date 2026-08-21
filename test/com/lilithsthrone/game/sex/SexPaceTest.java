package com.lilithsthrone.game.sex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Characterization test pinning {@link SexPace#getOppositeDomEquivalent()} for every enum value,
 * so the switch-statement -> switch-expression conversion is provably behavior-preserving.
 */
class SexPaceTest {

	@Test
	@DisplayName("getOppositeDomEquivalent maps every pace to its pinned opposite")
	void oppositeDomEquivalentIsStable() {
		assertThat(SexPace.DOM_GENTLE.getOppositeDomEquivalent()).isEqualTo(SexPace.SUB_NORMAL);
		assertThat(SexPace.DOM_NORMAL.getOppositeDomEquivalent()).isEqualTo(SexPace.SUB_NORMAL);
		assertThat(SexPace.DOM_ROUGH.getOppositeDomEquivalent()).isEqualTo(SexPace.SUB_EAGER);
		assertThat(SexPace.SUB_EAGER.getOppositeDomEquivalent()).isEqualTo(SexPace.DOM_ROUGH);
		assertThat(SexPace.SUB_NORMAL.getOppositeDomEquivalent()).isEqualTo(SexPace.DOM_NORMAL);
		assertThat(SexPace.SUB_RESISTING.getOppositeDomEquivalent()).isEqualTo(SexPace.DOM_GENTLE);
	}
}
