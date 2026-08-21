package com.lilithsthrone.game.dialogue.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.lilithsthrone.testutil.GameBootstrap;
import com.lilithsthrone.testutil.JavaFxHarness;

/**
 * Characterization tests for the content scripting engine via {@link UtilText#parse}.
 * These pin the observable input->output of the {@code #IF/#ELSEIF/#ELSE/#ENDIF} conditionals
 * (evaluated by Nashorn), which is the durable guard for any future scripting-engine change
 * (e.g. a Nashorn replacement). Inputs are self-contained and RNG-free.
 */
@ExtendWith(JavaFxHarness.class)
class UtilTextParseTest {

	@BeforeAll
	static void boot() {
		GameBootstrap.boot();
		UtilText.initScriptEngine();
	}

	@Test
	@DisplayName("plain text is returned unchanged")
	void plainTextPassesThrough() {
		assertThat(UtilText.parse("Hello, world.")).isEqualTo("Hello, world.");
	}

	@Test
	@DisplayName("#IF true branch is selected")
	void conditionalTrueBranch() {
		assertThat(UtilText.parse("#IF(true)A#ELSEB#ENDIF")).isEqualTo("A");
	}

	@Test
	@DisplayName("#IF false branch falls through to #ELSE")
	void conditionalFalseBranch() {
		assertThat(UtilText.parse("#IF(false)A#ELSEB#ENDIF")).isEqualTo("B");
	}

	@Test
	@DisplayName("#ELSEIF is selected when the first condition is false")
	void conditionalElseIfBranch() {
		assertThat(UtilText.parse("#IF(1>2)A#ELSEIF(3>2)B#ELSEC#ENDIF")).isEqualTo("B");
	}

	@Test
	@DisplayName("#ELSE is selected when all conditions are false")
	void conditionalElseFallthrough() {
		assertThat(UtilText.parse("#IF(1>2)A#ELSEIF(2>3)B#ELSEC#ENDIF")).isEqualTo("C");
	}

	@Test
	@DisplayName("arithmetic conditions are evaluated by the script engine")
	void conditionalArithmetic() {
		assertThat(UtilText.parse("#IF(1+1==2)math#ELSEwrong#ENDIF")).isEqualTo("math");
	}
}
