package com.lilithsthrone.game.character.body;

import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.abstractTypes.AbstractFluidType;
import com.lilithsthrone.game.character.body.valueEnums.FluidRegeneration;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.utils.Units;

/**
 * A character's bladder, which stores and regenerates {@link FluidUrine}. Modelled on the cum-storage behaviour of {@link Testicle}.
 *
 * @since 0.4.9.7
 * @version 0.4.9.7
 * @author Innoxia
 */
public class Bladder {

	public static final int DEFAULT_URINE_STORAGE = 500; // ml
	public static final int MAXIMUM_URINE_STORAGE = 10_000; // ml
	/** Below this value, all urine is expelled upon voiding the bladder. */
	public static final int MINIMUM_VALUE_FOR_ALL_URINE_TO_BE_EXPELLED = 5; // ml

	protected int urineStorage;
	protected float urineStored;
	/** Measured in mL/day */
	protected int urineRegeneration;

	protected FluidUrine urine;

	public Bladder(AbstractFluidType urineType, int urineStorage) {
		this.urineStorage = Math.max(0, Math.min(urineStorage, MAXIMUM_URINE_STORAGE));
		this.urineStored = this.urineStorage;
		this.urineRegeneration = FluidRegeneration.ONE_AVERAGE.getMedianRegenerationValuePerDay();

		this.urine = new FluidUrine(urineType);
	}

	public Bladder(Bladder bladderToCopy) {
		this.urineStorage = bladderToCopy.urineStorage;
		this.urineStored = bladderToCopy.urineStored;
		this.urineRegeneration = bladderToCopy.urineRegeneration;

		this.urine = new FluidUrine(bladderToCopy.urine);
	}

	public FluidUrine getUrine() {
		return urine;
	}

	public void setUrine(FluidUrine urine) {
		this.urine = urine;
	}

	public AbstractFluidType getType() {
		return urine.getType();
	}

	public void setType(AbstractFluidType type) {
		urine.setType(type);
	}

	// Storage:

	public int getRawUrineStorageValue() {
		return urineStorage;
	}

	/**
	 * Sets the maximum amount of urine this bladder can hold. Value is bound to >=0 && <={@link #MAXIMUM_URINE_STORAGE}.
	 */
	public String setUrineStorage(GameCharacter owner, int urineStorage) {
		int oldStorage = this.urineStorage;
		this.urineStorage = Math.max(0, Math.min(urineStorage, MAXIMUM_URINE_STORAGE));
		int change = this.urineStorage - oldStorage;

		if(this.urineStored > this.urineStorage) {
			this.urineStored = this.urineStorage;
		}

		if(owner==null) {
			return "";
		}

		if(change == 0) {
			return UtilText.parse(owner, "<p style='text-align:center;'>[style.colourDisabled(The amount of [npc.urine] which [npc.name] [npc.is] able to hold doesn't change...)]</p>");
		}

		if(change > 0) {
			return UtilText.parse(owner,
					"<p>"
						+ "[npc.Name] [npc.verb(feel)] a strange pressure building deep within [npc.her] abdomen,"
							+ " clear evidence that [npc.her] bladder capacity has [style.boldGrow(increased)].<br/>"
						+ "[npc.SheIsFull] now able to hold [style.boldSex(" + Units.fluid(this.urineStorage) + ")] of [npc.urine]!"
					+ "</p>");
		} else {
			return UtilText.parse(owner,
					"<p>"
						+ "[npc.Name] [npc.verb(feel)] a strange tightening deep within [npc.her] abdomen,"
							+ " clear evidence that [npc.her] bladder capacity has [style.boldShrink(decreased)].<br/>"
						+ "[npc.SheIsFull] now able to hold [style.boldSex(" + Units.fluid(this.urineStorage) + ")] of [npc.urine]!"
					+ "</p>");
		}
	}

	// Stored urine:

	public float getRawStoredUrineValue() {
		return urineStored;
	}

	/**
	 * Sets the amount of urine currently stored. Value is bound to >=0 && <={@link #getRawUrineStorageValue()}.
	 */
	public String setStoredUrine(GameCharacter owner, float urineStored, boolean withFormatting) {
		float oldStored = this.urineStored;
		this.urineStored = Math.max(0, Math.min(urineStored, getRawUrineStorageValue()));
		float change = oldStored - this.urineStored;

		if(owner==null) {
			return "";
		}

		if(change <= 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		if(withFormatting) {
			sb.append("<p style='text-align:center;'>[style.italicsMinorBad(");
		}
		sb.append(Units.fluid(change, Units.UnitType.LONG)+" of [npc.urine+] streams out of [npc.her] urethra.");
		if(withFormatting) {
			sb.append(")]");
		}
		if(this.urineStored==0) {
			sb.append("<br/><i>[npc.Name] now [npc.has] emptied [npc.her] bladder!</i>");
		}
		if(withFormatting) {
			sb.append("</p>");
		}
		return UtilText.parse(owner, sb.toString());
	}

	public String setStoredUrine(GameCharacter owner, float urineStored) {
		return setStoredUrine(owner, urineStored, true);
	}

	// Regeneration:

	public FluidRegeneration getUrineRegeneration() {
		return FluidRegeneration.getFluidRegenerationFromInt(urineRegeneration);
	}

	public int getRawUrineRegenerationValue() {
		return urineRegeneration;
	}

	/**
	 * Sets the urineRegeneration. Value is bound to >=0 && <=FluidRegeneration.FOUR_VERY_RAPID.getMaximumRegenerationValuePerDay()
	 */
	public String setUrineRegeneration(GameCharacter owner, int urineRegeneration) {
		int oldRegeneration = this.urineRegeneration;
		this.urineRegeneration = Math.max(0, Math.min(urineRegeneration, FluidRegeneration.FOUR_VERY_RAPID.getMaximumRegenerationValuePerDay()));
		int regenerationChange = this.urineRegeneration - oldRegeneration;

		if(owner==null) {
			return "";
		}

		if(regenerationChange == 0) {
			return UtilText.parse(owner, "<p style='text-align:center;'>[style.colourDisabled([npc.namePos] rate of [npc.urine] regeneration doesn't change...)]</p>");
		}

		String regenerationDescriptor = getUrineRegeneration().getName();
		if(regenerationChange > 0) {
			return UtilText.parse(owner,
					"<p>"
						+ "[npc.Name] [npc.verb(feel)] [npc.her] kidneys working harder deep within [npc.her] abdomen,"
							+ " clear evidence that [npc.her] [npc.urine] regeneration has [style.boldGrow(increased)].<br/>"
						+ "[npc.Her] rate of [npc.urine] regeneration is now [style.boldSex(" + regenerationDescriptor + ")] ("+Units.fluid(this.urineRegeneration)+"/day)!"
					+ "</p>");
		} else {
			return UtilText.parse(owner,
					"<p>"
						+ "[npc.Name] [npc.verb(feel)] [npc.her] kidneys slowing down deep within [npc.her] abdomen,"
							+ " clear evidence that [npc.her] [npc.urine] regeneration has [style.boldShrink(decreased)].<br/>"
						+ "[npc.Her] rate of [npc.urine] regeneration is now [style.boldSex(" + regenerationDescriptor + ")] ("+Units.fluid(this.urineRegeneration)+"/day)!"
					+ "</p>");
		}
	}
}
