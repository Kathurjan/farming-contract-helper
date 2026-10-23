package com.farmingcontract;

import com.farmingcontract.data.PatchType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Patch highlight name matching.
 *
 * <p>The Farming Guild contains several objects whose names share the substring "tree patch", which
 * is why matching is exact rather than substring-based. Reported in game: a Magic tree contract
 * highlighted three patches at once.
 */
public class PatchHighlightMatchingTest
{
	/**
	 * Every patch-like object name present in the Farming Guild.
	 */
	private static final String[] GUILD_OBJECT_NAMES = {
		"Allotment", "Flower patch", "Bush patch", "Herb patch", "Tree patch", "Fruit tree patch",
		"Cactus patch", "Celastrus patch", "Redwood tree patch", "Spirit tree patch", "Anima patch",
		"Compost bin", "Big compost bin",
	};

	private static int countMatches(PatchType type)
	{
		int matches = 0;
		for (String name : GUILD_OBJECT_NAMES)
		{
			if (PatchTracker.matchesName(name, type))
			{
				matches++;
			}
		}
		return matches;
	}

	@Test
	public void aTreeContractMatchesExactlyOneGuildPatch()
	{
		// The reported bug highlighted three: Tree patch, Spirit tree patch, Redwood tree patch.
		assertEquals("a tree contract must highlight exactly one patch", 1,
			countMatches(PatchType.TREE));
	}

	@Test
	public void everyPatchTypeMatchesExactlyOneGuildObject()
	{
		for (PatchType type : PatchType.values())
		{
			assertEquals(type + " must match exactly one guild object", 1, countMatches(type));
		}
	}

	@Test
	public void spiritTreeIsNotAnyContractPatch()
	{
		// "Spirit tree patch" contains "tree patch" but is never a contract patch.
		for (PatchType type : PatchType.values())
		{
			assertFalse(type.name(), PatchTracker.matchesName("Spirit tree patch", type));
		}
	}

	@Test
	public void redwoodIsItsOwnPatchAndNotATreePatch()
	{
		// "Redwood tree patch" also contains "tree patch", which both over-matched TREE and stole
		// the patch away from REDWOOD.
		assertFalse(PatchTracker.matchesName("Redwood tree patch", PatchType.TREE));
		assertTrue(PatchTracker.matchesName("Redwood tree patch", PatchType.REDWOOD));
	}

	@Test
	public void fruitTreeAndTreeAreNotConfused()
	{
		assertTrue(PatchTracker.matchesName("Fruit tree patch", PatchType.FRUIT_TREE));
		assertFalse(PatchTracker.matchesName("Fruit tree patch", PatchType.TREE));
		assertTrue(PatchTracker.matchesName("Tree patch", PatchType.TREE));
		assertFalse(PatchTracker.matchesName("Tree patch", PatchType.FRUIT_TREE));
	}

	@Test
	public void eachPatchTypeMatchesItsOwnObjectName()
	{
		assertTrue(PatchTracker.matchesName("Herb patch", PatchType.HERB));
		assertTrue(PatchTracker.matchesName("Flower patch", PatchType.FLOWER));
		assertTrue(PatchTracker.matchesName("Bush patch", PatchType.BUSH));
		assertTrue(PatchTracker.matchesName("Allotment", PatchType.ALLOTMENT));
		assertTrue(PatchTracker.matchesName("Cactus patch", PatchType.CACTUS));
		assertTrue(PatchTracker.matchesName("Celastrus patch", PatchType.CELASTRUS));
	}

	@Test
	public void matchingIsCaseAndWhitespaceInsensitive()
	{
		assertTrue(PatchTracker.matchesName("HERB PATCH", PatchType.HERB));
		assertTrue(PatchTracker.matchesName("herb patch", PatchType.HERB));
		assertTrue(PatchTracker.matchesName("  Herb patch  ", PatchType.HERB));
	}

	@Test
	public void unrelatedObjectsNeverMatch()
	{
		for (PatchType type : PatchType.values())
		{
			assertFalse(type.name(), PatchTracker.matchesName("Bank booth", type));
			assertFalse(type.name(), PatchTracker.matchesName("Compost bin", type));
			assertFalse(type.name(), PatchTracker.matchesName("Anima patch", type));
		}
	}

	@Test
	public void differentPatchTypesDoNotCrossMatch()
	{
		assertFalse(PatchTracker.matchesName("Herb patch", PatchType.FLOWER));
		assertFalse(PatchTracker.matchesName("Flower patch", PatchType.HERB));
		assertFalse(PatchTracker.matchesName("Bush patch", PatchType.CACTUS));
		assertFalse(PatchTracker.matchesName("Allotment", PatchType.HERB));
	}
}
