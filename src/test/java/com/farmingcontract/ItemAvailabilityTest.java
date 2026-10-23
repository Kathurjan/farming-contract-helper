package com.farmingcontract;

import com.farmingcontract.ItemAvailability.Status;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Inventory quantity calculations, noted-item handling, and the honesty rules around unloaded
 * container data. Spec test category 6.
 */
public class ItemAvailabilityTest
{
	/**
	 * A gardener payment: notes are accepted.
	 */
	private static ItemAvailability payment(int required, int carried, int noted, int bank, int vault)
	{
		return ItemAvailability.of(required, carried, noted, bank, vault, true);
	}

	/**
	 * A seed or sapling: notes are not plantable.
	 */
	private static ItemAvailability seed(int required, int carried, int noted, int bank, int vault)
	{
		return ItemAvailability.of(required, carried, noted, bank, vault, false);
	}

	// -------------------------------------------------- carried

	@Test
	public void satisfiedWhenInventoryMeetsTheRequirement()
	{
		ItemAvailability a = payment(5, 5, 0, -1, -1);
		assertTrue(a.isSatisfiedFromInventory());
		assertEquals(Status.HAVE, a.getStatus());
		assertEquals("5 / 5", a.getInventoryProgress());
	}

	@Test
	public void shortWhenInventoryIsBelowTheRequirement()
	{
		ItemAvailability a = payment(5, 2, 0, 0, 0);
		assertFalse(a.isSatisfiedFromInventory());
		assertEquals(Status.MISSING, a.getStatus());
	}

	// -------------------------------------------------- noted items

	@Test
	public void notedPaymentCountsAsAvailable()
	{
		// 25 noted coconuts pay for a magic tree just as well as 25 loose ones.
		ItemAvailability a = payment(25, 0, 25, -1, -1);
		assertTrue(a.isSatisfiedFromInventory());
		assertEquals(Status.HAVE_NOTED, a.getStatus());
		assertTrue(a.isCoveredByNote());
	}

	@Test
	public void notedAndLoosePaymentsCombine()
	{
		ItemAvailability a = payment(25, 10, 15, -1, -1);
		assertTrue(a.isSatisfiedFromInventory());
		assertEquals(25, a.getUsableInInventory());
		assertEquals(Status.HAVE_NOTED, a.getStatus());
	}

	@Test
	public void notedSeedDoesNotCountAsPlantable()
	{
		// A noted sapling cannot be planted, so this is not satisfied.
		ItemAvailability a = seed(1, 0, 1, 0, 0);
		assertFalse(a.isSatisfiedFromInventory());
		assertEquals(0, a.getUsableInInventory());
		assertTrue(a.hasUnusableNote());
		assertEquals(Status.MISSING, a.getStatus());
	}

	@Test
	public void unnotedSeedIsPlantable()
	{
		ItemAvailability a = seed(3, 3, 0, -1, -1);
		assertTrue(a.isSatisfiedFromInventory());
		assertFalse(a.hasUnusableNote());
		assertEquals(Status.HAVE, a.getStatus());
	}

	@Test
	public void carriedItemIsNotReportedAsCoveredByANote()
	{
		ItemAvailability a = payment(5, 5, 3, -1, -1);
		assertEquals(Status.HAVE, a.getStatus());
		assertFalse(a.isCoveredByNote());
	}

	// -------------------------------------------------- bank and vault

	@Test
	public void itemsInBankAreShownAsInStorageNotMissing()
	{
		// The behaviour asked for: having it in the bank is not the same as not having it.
		ItemAvailability a = payment(25, 0, 0, 25, 0);
		assertEquals(Status.IN_STORAGE, a.getStatus());
		assertFalse(a.isDefinitelyShort());
		assertEquals("bank: 25", a.getStorageDescription());
	}

	@Test
	public void seedVaultCountsAsStorage()
	{
		ItemAvailability a = seed(3, 0, 0, 0, 3);
		assertEquals(Status.IN_STORAGE, a.getStatus());
		assertEquals("vault: 3", a.getStorageDescription());
	}

	@Test
	public void bankAndVaultAreBothDescribed()
	{
		ItemAvailability a = seed(10, 0, 0, 4, 6);
		assertEquals(Status.IN_STORAGE, a.getStatus());
		assertEquals("bank: 4, vault: 6", a.getStorageDescription());
	}

	@Test
	public void inventoryAndStorageCombineToSatisfy()
	{
		ItemAvailability a = payment(5, 1, 0, 3, 1);
		assertFalse(a.isSatisfiedFromInventory());
		assertTrue(a.isSatisfiedFromKnownSources());
		assertEquals(Status.IN_STORAGE, a.getStatus());
	}

	// -------------------------------------------------- unknown sources

	@Test
	public void unopenedBankIsUnknownNotEmpty()
	{
		ItemAvailability a = payment(5, 2, 0, -1, -1);
		assertFalse(a.isBankKnown());
		assertFalse(a.isSeedVaultKnown());
		assertFalse(a.isSourcesLoaded());
		// Short in the inventory, but we must NOT claim the player lacks the item.
		assertFalse(a.isDefinitelyShort());
		assertEquals(Status.UNKNOWN, a.getStatus());
	}

	@Test
	public void definitelyShortOnlyWhenEverySourceHasBeenSeen()
	{
		assertTrue(payment(5, 2, 0, 0, 0).isDefinitelyShort());
		assertFalse(payment(5, 2, 0, 0, -1).isDefinitelyShort());
		assertEquals(Status.UNKNOWN, payment(5, 2, 0, 0, -1).getStatus());
	}

	@Test
	public void knownTotalSumsOnlyLoadedSources()
	{
		assertEquals(2, payment(5, 2, 0, -1, -1).getKnownTotal());
		assertEquals(5, payment(5, 2, 0, 3, -1).getKnownTotal());
		assertEquals(9, payment(5, 2, 0, 3, 4).getKnownTotal());
	}

	@Test
	public void storageDescriptionIsNullWhenNothingKnown()
	{
		assertNull(payment(5, 0, 0, -1, -1).getStorageDescription());
		assertNull(payment(5, 0, 0, 0, 0).getStorageDescription());
	}

	// -------------------------------------------------- nothing required

	@Test
	public void nothingRequiredIsNotAShortfall()
	{
		ItemAvailability none = payment(0, 0, 0, -1, -1);
		assertTrue(none.isNotRequired());
		assertTrue(none.isSatisfiedFromInventory());
		assertEquals(Status.NOT_REQUIRED, none.getStatus());
	}

	@Test
	public void unknownFactoryRequiresNothing()
	{
		ItemAvailability a = ItemAvailability.unknown();
		assertTrue(a.isNotRequired());
		assertFalse(a.isBankKnown());
		assertFalse(a.isSeedVaultKnown());
		assertEquals(Status.NOT_REQUIRED, a.getStatus());
	}
}
