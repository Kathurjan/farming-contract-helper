package com.farmingcontract.data;

import com.farmingcontract.farming.PatchImplementation;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

/**
 * The Farming Guild patch types that can host a farming contract crop.
 *
 * <p>Varbit assignments and patch/region layout are taken from RuneLite's
 * {@code FarmingWorld} definition of the Farming Guild (region 4922). Allotments are the only
 * duplicated patch type in the guild, so they carry two varbits.
 */
@Getter
public enum PatchType
{
	ALLOTMENT(
		"Allotment",
		"Allotment",
		"Allotment patch, Farming Guild lower tier",
		PatchImplementation.ALLOTMENT,
		3,
		VarbitID.FARMING_TRANSMIT_C,
		VarbitID.FARMING_TRANSMIT_D),

	FLOWER(
		"Flower",
		"Flower patch",
		"Flower patch, Farming Guild lower tier",
		PatchImplementation.FLOWER,
		1,
		VarbitID.FARMING_TRANSMIT_H),

	BUSH(
		"Bush",
		"Bush patch",
		"Bush patch, Farming Guild lower tier",
		PatchImplementation.BUSH,
		1,
		VarbitID.FARMING_TRANSMIT_B),

	HERB(
		"Herb",
		"Herb patch",
		"Herb patch, Farming Guild middle tier",
		PatchImplementation.HERB,
		1,
		VarbitID.FARMING_TRANSMIT_E),

	TREE(
		"Tree",
		"Tree patch",
		"Tree patch, west side of Farming Guild middle tier",
		PatchImplementation.TREE,
		1,
		VarbitID.FARMING_TRANSMIT_G),

	FRUIT_TREE(
		"Fruit tree",
		"Fruit tree patch",
		"Fruit tree patch, Farming Guild upper tier",
		PatchImplementation.FRUIT_TREE,
		1,
		VarbitID.FARMING_TRANSMIT_K),

	CACTUS(
		"Cactus",
		"Cactus patch",
		"Cactus patch, Farming Guild upper tier",
		PatchImplementation.CACTUS,
		1,
		VarbitID.FARMING_TRANSMIT_F),

	CELASTRUS(
		"Celastrus",
		"Celastrus patch",
		"Celastrus patch, Farming Guild upper tier",
		PatchImplementation.CELASTRUS,
		1,
		VarbitID.FARMING_TRANSMIT_L),

	REDWOOD(
		"Redwood",
		"Redwood tree patch",
		"Redwood patch, Farming Guild upper tier",
		PatchImplementation.REDWOOD,
		1,
		VarbitID.FARMING_TRANSMIT_I);

	private final String displayName;

	/**
	 * Exact scene object name for this patch in the Farming Guild.
	 *
	 * <p>Matched with equality, not substring: the guild also contains a "Spirit tree patch" and a
	 * "Redwood tree patch", both of which contain the substring "tree patch" and would otherwise be
	 * highlighted for an ordinary tree contract.
	 */
	private final String objectName;

	/**
	 * Human-readable location, understandable without a map.
	 */
	private final String locationDescription;

	/**
	 * Vendored RuneLite decoder used to turn a patch varbit value into produce + crop state.
	 */
	private final PatchImplementation implementation;

	/**
	 * How many seeds or saplings one planting of this patch type consumes.
	 * Allotments take three seeds; every other contract patch takes one.
	 */
	private final int plantQuantity;

	private final int[] varbits;

	PatchType(String displayName, String objectName, String locationDescription,
		PatchImplementation implementation, int plantQuantity, int... varbits)
	{
		this.displayName = displayName;
		this.objectName = objectName;
		this.locationDescription = locationDescription;
		this.implementation = implementation;
		this.plantQuantity = plantQuantity;
		this.varbits = varbits;
	}

	public List<Integer> getVarbitList()
	{
		List<Integer> out = new java.util.ArrayList<>(varbits.length);
		for (int v : varbits)
		{
			out.add(v);
		}
		return Collections.unmodifiableList(out);
	}
}
