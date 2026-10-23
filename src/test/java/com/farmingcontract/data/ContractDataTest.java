package com.farmingcontract.data;

import com.farmingcontract.farming.Produce;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;

/**
 * Static contract data: crop to patch, crop to seed, seed quantities, protection payments and
 * basket/sack quantities. Spec test categories 1-5 and 14.
 */
public class ContractDataTest
{
	// -------------------------------------------------- 1. crop to patch mapping

	@Test
	public void cropsMapToTheCorrectPatchType()
	{
		assertEquals(PatchType.TREE, ContractCrop.MAPLE.getPatchType());
		assertEquals(PatchType.FRUIT_TREE, ContractCrop.PAPAYA.getPatchType());
		assertEquals(PatchType.HERB, ContractCrop.SNAPDRAGON.getPatchType());
		assertEquals(PatchType.ALLOTMENT, ContractCrop.WATERMELON.getPatchType());
		assertEquals(PatchType.FLOWER, ContractCrop.WHITE_LILY.getPatchType());
		assertEquals(PatchType.BUSH, ContractCrop.POISON_IVY.getPatchType());
		assertEquals(PatchType.CACTUS, ContractCrop.POTATO_CACTUS.getPatchType());
		assertEquals(PatchType.CELASTRUS, ContractCrop.CELASTRUS.getPatchType());
		assertEquals(PatchType.REDWOOD, ContractCrop.REDWOOD.getPatchType());
	}

	@Test
	public void everyPatchTypeHasAtLeastOneVarbitAndALocation()
	{
		for (PatchType type : PatchType.values())
		{
			assertFalse(type.name(), type.getVarbitList().isEmpty());
			assertFalse(type.name(), type.getLocationDescription().isEmpty());
			assertNotNull(type.name(), type.getImplementation());
		}
	}

	@Test
	public void onlyAllotmentIsDuplicatedInTheGuild()
	{
		assertEquals(2, PatchType.ALLOTMENT.getVarbitList().size());
		for (PatchType type : PatchType.values())
		{
			if (type != PatchType.ALLOTMENT)
			{
				assertEquals(type.name(), 1, type.getVarbitList().size());
			}
		}
	}

	// -------------------------------------------------- 2. crop to seed mapping

	@Test
	public void cropsMapToTheCorrectSeedOrSapling()
	{
		assertEquals(ItemID.PLANTPOT_MAPLE_SAPLING, ContractCrop.MAPLE.getSeedItemId());
		assertEquals(ItemID.PLANTPOT_OAK_SAPLING, ContractCrop.OAK.getSeedItemId());
		assertEquals(ItemID.RANARR_SEED, ContractCrop.RANARR.getSeedItemId());
		assertEquals(ItemID.WATERMELON_SEED, ContractCrop.WATERMELON.getSeedItemId());
		assertEquals(ItemID.POTATO_CACTUS_SEED, ContractCrop.POTATO_CACTUS.getSeedItemId());
		assertEquals(ItemID.PLANTPOT_REDWOOD_TREE_SAPLING, ContractCrop.REDWOOD.getSeedItemId());
	}

	@Test
	public void treeContractsRequireSaplingsNotRawSeeds()
	{
		// Jane's contract is satisfied by planting a sapling; the raw seed must be potted first.
		assertEquals(ItemID.PLANTPOT_OAK_SAPLING, ContractCrop.OAK.getSeedItemId());
		assertEquals(ItemID.PLANTPOT_APPLE_SAPLING, ContractCrop.APPLE.getSeedItemId());
		assertEquals(ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING, ContractCrop.CELASTRUS.getSeedItemId());
		assertTrue(ContractCrop.OAK.getSeedName().endsWith("sapling"));
		assertTrue(ContractCrop.DRAGONFRUIT.getSeedName().endsWith("sapling"));
	}

	@Test
	public void everyCropHasADistinctNonZeroSeedName()
	{
		Set<String> names = new HashSet<>();
		for (ContractCrop crop : ContractCrop.values())
		{
			assertTrue(crop.name(), crop.getSeedItemId() > 0);
			assertFalse(crop.name(), crop.getSeedName().isEmpty());
			assertTrue("duplicate seed name " + crop.getSeedName(), names.add(crop.getSeedName()));
		}
	}

	// -------------------------------------------------- 3. seed quantities

	@Test
	public void allotmentsTakeThreeSeedsEverythingElseTakesOne()
	{
		assertEquals(3, ContractCrop.POTATO.getSeedQuantity());
		assertEquals(3, ContractCrop.WATERMELON.getSeedQuantity());
		assertEquals(3, ContractCrop.SNAPE_GRASS.getSeedQuantity());

		for (ContractCrop crop : ContractCrop.values())
		{
			int expected = crop.getPatchType() == PatchType.ALLOTMENT ? 3 : 1;
			assertEquals(crop.name(), expected, crop.getSeedQuantity());
		}
	}

	// -------------------------------------------------- 4. protection payment mappings

	@Test
	public void protectionPaymentsAreCropSpecific()
	{
		// The spec's worked example.
		ProtectionPayment maple = ContractCrop.MAPLE.getProtection();
		assertTrue(maple.isAvailable());
		assertEquals(ItemID.BASKET_ORANGE_5, maple.getItemId());
		assertEquals(1, maple.getQuantity());
		assertEquals("1 Basket of oranges", maple.getDescription());
		assertEquals(5, maple.getLooseQuantity());
		assertEquals(ItemID.ORANGE, maple.getLooseItemId());
		assertEquals("5 Oranges", maple.getContentsDescription());
	}

	@Test
	public void loosePaymentsCarryNoContainer()
	{
		ProtectionPayment yew = ContractCrop.YEW.getProtection();
		assertTrue(yew.isAvailable());
		assertFalse(yew.isContainerRequired());
		assertEquals(ItemID.CACTUS_SPINE, yew.getItemId());
		assertEquals(10, yew.getQuantity());
		assertNull(yew.getContentsDescription());

		ProtectionPayment magic = ContractCrop.MAGIC.getProtection();
		assertEquals(ItemID.COCONUT, magic.getItemId());
		assertEquals(25, magic.getQuantity());

		ProtectionPayment redwood = ContractCrop.REDWOOD.getProtection();
		assertEquals(ItemID.DRAGONFRUIT, redwood.getItemId());
		assertEquals(6, redwood.getQuantity());
	}

	@Test
	public void potatoIsPaidInCompost()
	{
		ProtectionPayment potato = ContractCrop.POTATO.getProtection();
		assertEquals(ItemID.BUCKET_COMPOST, potato.getItemId());
		assertEquals(2, potato.getQuantity());
		assertFalse(potato.isContainerRequired());
	}

	// -------------------------------------------------- 5. basket and sack quantities

	@Test
	public void basketsHoldFiveAndSacksHoldTen()
	{
		assertEquals(5, ProtectionPayment.BASKET_CAPACITY);
		assertEquals(10, ProtectionPayment.SACK_CAPACITY);
	}

	@Test
	public void multipleContainersMultiplyTheLooseQuantity()
	{
		// Curry: 5 baskets of bananas = 25 bananas.
		ProtectionPayment curry = ContractCrop.CURRY.getProtection();
		assertEquals(5, curry.getQuantity());
		assertEquals(25, curry.getLooseQuantity());
		assertEquals("25 Bananas", curry.getContentsDescription());

		// Redberries: 4 sacks of cabbages = 40 cabbages.
		ProtectionPayment redberries = ContractCrop.REDBERRIES.getProtection();
		assertEquals(4, redberries.getQuantity());
		assertEquals(40, redberries.getLooseQuantity());
		assertEquals(ItemID.SACK_CABBAGE_10, redberries.getItemId());

		// Tomato: 2 sacks of cabbages = 20 cabbages.
		assertEquals(20, ContractCrop.TOMATO.getProtection().getLooseQuantity());
	}

	@Test
	public void containerPaymentsUseTheFilledContainerId()
	{
		for (ContractCrop crop : ContractCrop.values())
		{
			ProtectionPayment payment = crop.getProtection();
			if (payment.isContainerRequired())
			{
				int perContainer = payment.getLooseQuantity() / payment.getQuantity();
				assertTrue(crop.name() + " container capacity " + perContainer,
					perContainer == ProtectionPayment.BASKET_CAPACITY
						|| perContainer == ProtectionPayment.SACK_CAPACITY);
			}
		}
	}

	// -------------------------------------------------- 14. crops without protection

	@Test
	public void herbsAndFlowersCannotBeProtected()
	{
		for (ContractCrop crop : ContractCrop.values())
		{
			if (crop.getPatchType() == PatchType.HERB || crop.getPatchType() == PatchType.FLOWER)
			{
				assertSame(crop.name(), ProtectionPayment.NONE, crop.getProtection());
				assertFalse(crop.name(), crop.getProtection().isAvailable());
				assertEquals("Not available", crop.getProtection().getDescription());
			}
		}
	}

	@Test
	public void poisonIvyNeedsNoProtection()
	{
		// Poison ivy cannot become diseased, so there is nothing to protect against.
		assertFalse(ContractCrop.POISON_IVY.getProtection().isAvailable());
	}

	@Test
	public void everyNonHerbNonFlowerCropExceptPoisonIvyHasAPayment()
	{
		for (ContractCrop crop : ContractCrop.values())
		{
			boolean expectedNone = crop.getPatchType() == PatchType.HERB
				|| crop.getPatchType() == PatchType.FLOWER
				|| crop == ContractCrop.POISON_IVY;
			assertEquals(crop.name(), !expectedNone, crop.getProtection().isAvailable());
		}
	}

	// -------------------------------------------------- lookups and tiers

	@Test
	public void contractNameLookupIsCaseInsensitive()
	{
		assertSame(ContractCrop.MAPLE, ContractCrop.forContractName("Maple tree"));
		assertSame(ContractCrop.MAPLE, ContractCrop.forContractName("maple tree"));
		assertSame(ContractCrop.MAPLE, ContractCrop.forContractName("  MAPLE TREE  "));
		assertNull(ContractCrop.forContractName("Banana bread"));
		assertNull(ContractCrop.forContractName(null));
	}

	@Test
	public void produceLookupResolvesBackToTheCrop()
	{
		assertSame(ContractCrop.MAPLE, ContractCrop.forProduce(Produce.MAPLE));
		assertSame(ContractCrop.TORSTOL, ContractCrop.forProduce(Produce.TORSTOL));
		assertNull(ContractCrop.forProduce(null));
		// Weeds are not a contract crop.
		assertNull(ContractCrop.forProduce(Produce.WEEDS));
	}

	@Test
	public void everyCropBelongsToAtLeastOneTier()
	{
		for (ContractCrop crop : ContractCrop.values())
		{
			assertFalse(crop.name(), crop.getTiers().isEmpty());
		}
	}

	@Test
	public void representativeEasyMediumAndHardContracts()
	{
		// Easy
		assertTrue(ContractCrop.POTATO.getTiers().contains(ContractTier.EASY));
		assertEquals(PatchType.ALLOTMENT, ContractCrop.POTATO.getPatchType());

		// Medium
		assertTrue(ContractCrop.PAPAYA.getTiers().contains(ContractTier.MEDIUM));
		assertEquals(ItemID.PINEAPPLE, ContractCrop.PAPAYA.getProtection().getItemId());
		assertEquals(10, ContractCrop.PAPAYA.getProtection().getQuantity());

		// Hard
		assertTrue(ContractCrop.TORSTOL.getTiers().contains(ContractTier.HARD));
		assertFalse(ContractCrop.TORSTOL.getProtection().isAvailable());
		assertTrue(ContractCrop.REDWOOD.getTiers().contains(ContractTier.HARD));

		// Maple spans all three tiers.
		assertEquals(EnumSet.allOf(ContractTier.class), ContractCrop.MAPLE.getTiers());
	}

	@Test
	public void everyCropsProduceNameMatchesItsContractName()
	{
		for (ContractCrop crop : ContractCrop.values())
		{
			assertEquals(crop.name(), crop.getProduce().getContractName(), crop.getContractName());
			assertTrue(crop.name(), crop.getProduceItemId() > 0);
		}
	}
}
