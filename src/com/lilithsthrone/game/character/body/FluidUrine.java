package com.lilithsthrone.game.character.body;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.abstractTypes.AbstractFluidType;
import com.lilithsthrone.game.character.body.types.FluidType;
import com.lilithsthrone.game.character.body.valueEnums.FluidFlavour;
import com.lilithsthrone.game.character.body.valueEnums.FluidModifier;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.utils.Util;

/**
 * The urine produced by a character's bladder. Modelled on {@link FluidCum} and {@link FluidMilk}.
 *
 * @since 0.4.9.7
 * @version 0.4.9.7
 * @author Innoxia
 */
public class FluidUrine implements FluidInterface {

	protected AbstractFluidType type;
	protected FluidFlavour flavour;
	protected Set<FluidModifier> fluidModifiers;
	protected List<ItemEffect> transformativeEffects;

	public FluidUrine(AbstractFluidType type) {
		this.type = type;
		this.flavour = type.getFlavour();
		transformativeEffects = new ArrayList<>();

		fluidModifiers = new HashSet<>();
		fluidModifiers.addAll(type.getDefaultFluidModifiers());
	}

	public FluidUrine(FluidUrine urineToCopy) {
		this.type = urineToCopy.type;
		this.flavour = urineToCopy.flavour;
		this.fluidModifiers = new HashSet<>(urineToCopy.fluidModifiers);
		this.transformativeEffects = new ArrayList<>(urineToCopy.transformativeEffects);
	}

	public Element saveAsXML(String rootElementName, Element parentElement, Document doc) {
		Element element = doc.createElement(rootElementName);
		parentElement.appendChild(element);

		XMLUtil.addAttribute(doc, element, "type", FluidType.getIdFromFluidType(this.type));
		XMLUtil.addAttribute(doc, element, "flavour", this.flavour.toString());

		for(FluidModifier fm : this.getFluidModifiers()) {
			Element mod = doc.createElement("mod");
			mod.setTextContent(fm.toString());
			element.appendChild(mod);
		}

		return element;
	}

	public static FluidUrine loadFromXML(String rootElementName, Element parentElement, Document doc) {
		return loadFromXML(rootElementName, parentElement, doc, null);
	}

	/**
	 * @param baseType If you pass in a baseType, this method will ignore the saved type in parentElement.
	 */
	public static FluidUrine loadFromXML(String rootElementName, Element parentElement, Document doc, AbstractFluidType baseType) {
		Element urine = (Element)parentElement.getElementsByTagName(rootElementName).item(0);

		AbstractFluidType fluidType = FluidType.URINE_HUMAN;

		if(baseType!=null) {
			fluidType = baseType;

		} else if(urine!=null) {
			try {
				fluidType = FluidType.getFluidTypeFromId(urine.getAttribute("type"));
			} catch(Exception ex) {
			}
		}

		FluidUrine fluidUrine = new FluidUrine(fluidType);

		if(urine==null) {
			return fluidUrine;
		}

		String flavourId = urine.getAttribute("flavour");
		try {
			fluidUrine.flavour = FluidFlavour.valueOf(flavourId);
		} catch(Exception ex) {
			fluidUrine.flavour = FluidFlavour.URINE;
		}

		fluidUrine.fluidModifiers.clear();
		NodeList mods = urine.getElementsByTagName("mod");
		for(int i = 0; i < mods.getLength(); i++) {
			Element e = ((Element)mods.item(i));
			try {
				fluidUrine.fluidModifiers.add(FluidModifier.valueOf(e.getTextContent()));
			} catch(Exception ex) {
			}
		}

		return fluidUrine;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof FluidUrine other){
			if(other.getType().equals(this.getType())
				&& other.getFlavour() == this.getFlavour()
				&& other.getFluidModifiers().equals(this.getFluidModifiers())
				&& other.getTransformativeEffects().equals(this.getTransformativeEffects())){
					return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + this.getType().hashCode();
		result = 31 * result + this.getFlavour().hashCode();
		result = 31 * result + this.getFluidModifiers().hashCode();
		result = 31 * result + this.getTransformativeEffects().hashCode();
		return result;
	}

	@Override
	public String getDeterminer(GameCharacter gc) {
		return type.getDeterminer(gc);
	}

	@Override
	public String getName(GameCharacter gc) {
		return this.getType().getName(false, gc);
	}

	@Override
	public String getNameSingular(GameCharacter gc) {
		return this.getType().getNameSingular(gc);
	}

	@Override
	public String getNamePlural(GameCharacter gc) {
		return this.getType().getNamePlural(gc);
	}

	@Override
	public String getDescriptor(GameCharacter gc) {
		String modifierDescriptor = "";
		if(!fluidModifiers.isEmpty()) {
			modifierDescriptor = new ArrayList<>(fluidModifiers).get(Util.random.nextInt(fluidModifiers.size())).getName();
		}

		return UtilText.returnStringAtRandom(
				"warm",
				modifierDescriptor,
				flavour.getRandomFlavourDescriptor(),
				type.getDescriptor(gc));
	}

	@Override
	public AbstractFluidType getType() {
		return type;
	}

	public void setType(AbstractFluidType type) {
		this.type = type;
	}

	public FluidFlavour getFlavour() {
		return flavour;
	}

	public String setFlavour(GameCharacter owner, FluidFlavour flavour) {
		if(owner==null) {
			this.flavour = flavour;
			return "";
		}

		if(this.flavour == flavour || !owner.isAbleToUrinate()) {
			return "<p style='text-align:center;'>[style.colourDisabled(Nothing happens...)]</p>";
		}

		this.flavour = flavour;

		return UtilText.parse(owner,
				"<p>"
					+ "A soothing warmth spreads through [npc.namePos] bladder, causing [npc.herHim] to let out a contented little sigh.<br/>"
					+ "[npc.NamePos] [npc.urine] "
					+ (flavour==FluidFlavour.FLAVOURLESS
						?"is now <b style='color:"+flavour.getColour().toWebHexString()+";'>"+flavour.getName()+"</b>"
						:"now tastes of <b style='color:"+flavour.getColour().toWebHexString()+";'>"+flavour.getName()+"</b>.")
				+ "</p>");
	}

	public boolean hasFluidModifier(FluidModifier fluidModifier) {
		return fluidModifiers.contains(fluidModifier);
	}

	public String addFluidModifier(GameCharacter owner, FluidModifier fluidModifier) {
		if(owner==null && !fluidModifiers.contains(fluidModifier)) {
			fluidModifiers.add(fluidModifier);
			return "";
		}

		if(fluidModifiers.contains(fluidModifier) || !owner.isAbleToUrinate()) {
			return "<p style='text-align:center;'>[style.colourDisabled(Nothing happens...)]</p>";
		}

		if(fluidModifier==FluidModifier.ALCOHOLIC) {
			fluidModifiers.remove(FluidModifier.ALCOHOLIC_WEAK);
		} else if(fluidModifier==FluidModifier.ALCOHOLIC_WEAK) {
			fluidModifiers.remove(FluidModifier.ALCOHOLIC);
		}
		fluidModifiers.add(fluidModifier);

		return UtilText.parse(owner,
				"<p>"
					+ "A strange warmth spreads through [npc.namePos] bladder, causing [npc.herHim] to let out [npc.a_moan+].<br/>"
					+ "[npc.NamePos] [npc.urine] is now [style.boldGrow("+fluidModifier.getName()+")]!"
				+ "</p>");
	}

	public String removeFluidModifier(GameCharacter owner, FluidModifier fluidModifier) {
		if(owner==null) {
			fluidModifiers.remove(fluidModifier);
			return "";
		}

		if(!fluidModifiers.contains(fluidModifier) || !owner.isAbleToUrinate()) {
			return "<p style='text-align:center;'>[style.colourDisabled(Nothing happens...)]</p>";
		}

		fluidModifiers.remove(fluidModifier);

		return UtilText.parse(owner,
				"<p>"
					+ "A soft coolness spreads through [npc.namePos] bladder, causing [npc.herHim] to let out a gentle sigh.<br/>"
					+ "[npc.NamePos] [npc.urine] is [style.boldShrink(no longer "+fluidModifier.getName()+")]!"
				+ "</p>");
	}

	public List<ItemEffect> getTransformativeEffects() {
		return transformativeEffects;
	}

	public void addTransformativeEffect(ItemEffect ie) {
		transformativeEffects.add(ie);
	}

	/**
	 * DO NOT MODIFY!
	 */
	public Set<FluidModifier> getFluidModifiers() {
		return fluidModifiers;
	}

	public void clearFluidModifiers() {
		fluidModifiers.clear();
	}

	public float getValuePerMl() {
		return 0.01f * type.getValueModifier();
	}

	@Override
	public boolean isFeral(GameCharacter owner) {
		if(owner==null) {
			return false;
		}
		return owner.isFeral();
	}
}
