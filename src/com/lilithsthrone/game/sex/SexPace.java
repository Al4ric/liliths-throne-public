package com.lilithsthrone.game.sex;

import com.lilithsthrone.utils.colours.BaseColour;

/**
 * @since 0.1.69.9
 * @version 0.3.1
 * @author Innoxia
 */
public enum SexPace {
	
	SUB_RESISTING(false, "resisting", BaseColour.CRIMSON),
	SUB_NORMAL(false, "normal", BaseColour.PINK),
	SUB_EAGER(false, "eager", BaseColour.PINK_DEEP),
	
	DOM_GENTLE(true, "gentle", BaseColour.PINK_LIGHT),
	DOM_NORMAL(true, "normal", BaseColour.PINK),
	DOM_ROUGH(true, "rough", BaseColour.CRIMSON);
	
	private boolean isDom;
	private String name;
	private BaseColour colour;
	
	private SexPace(boolean isDom, String name, BaseColour colour) {
		this.isDom = isDom;
		this.name = name;
		this.colour = colour;
	}
	
	public SexPace getOppositeDomEquivalent() {
		return switch(this) {
			case DOM_GENTLE -> SUB_NORMAL;
			case DOM_NORMAL -> SUB_NORMAL;
			case DOM_ROUGH -> SUB_EAGER;
			case SUB_EAGER -> DOM_ROUGH;
			case SUB_NORMAL -> DOM_NORMAL;
			case SUB_RESISTING -> DOM_GENTLE;
		};
	}
	
	public boolean isDom() {
		return isDom;
	}

	public String getName() {
		return name;
	}

	public BaseColour getColour() {
		return colour;
	}
}
