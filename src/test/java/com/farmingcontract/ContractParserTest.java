package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Contract dialogue and game-message parsing. Spec test categories 7 and 8.
 */
public class ContractParserTest
{
	// -------------------------------------------------- 7. contract dialogue parsing

	@Test
	public void parsesTheWeNeedYouToGrowWording()
	{
		assertSame(ContractCrop.MAPLE,
			ContractParser.parseAssignment("We need you to grow a Maple tree for us?"));
		assertSame(ContractCrop.GUAM,
			ContractParser.parseAssignment("We need you to grow some Guam for us?"));
	}

	@Test
	public void parsesThePleaseCouldYouGrowWording()
	{
		assertSame(ContractCrop.SNAPDRAGON,
			ContractParser.parseAssignment("Please could you grow some Snapdragon for us?"));
		assertSame(ContractCrop.APPLE,
			ContractParser.parseAssignment("Please could you grow an Apple tree for us?"));
	}

	@Test
	public void parsesTheFullStopVariant()
	{
		assertSame(ContractCrop.WATERMELON,
			ContractParser.parseAssignment("We need you to grow some Watermelons."));
	}

	/**
	 * Jane names several crops in the plural ("Potatoes", "Watermelons", "Potato cacti"), which is
	 * what the contract-name table keys on. Singular forms are not contract names.
	 */
	@Test
	public void parsesPluralContractNames()
	{
		assertSame(ContractCrop.POTATO,
			ContractParser.parseAssignment("We need you to grow some Potatoes for us?"));
		assertSame(ContractCrop.STRAWBERRY,
			ContractParser.parseAssignment("We need you to grow some Strawberries for us?"));
		assertSame(ContractCrop.POTATO_CACTUS,
			ContractParser.parseAssignment("We need you to grow some Potato cacti for us?"));
		assertNull("the singular is not the contract name",
			ContractParser.parseAssignment("We need you to grow some Potato for us?"));
	}

	@Test
	public void parsesEasyMediumAndHardExamples()
	{
		// Easy
		assertSame(ContractCrop.POTATO,
			ContractParser.parseAssignment("We need you to grow some Potatoes for us?"));
		// Medium
		assertSame(ContractCrop.PAPAYA,
			ContractParser.parseAssignment("We need you to grow a Papaya tree for us?"));
		// Hard
		assertSame(ContractCrop.REDWOOD,
			ContractParser.parseAssignment("We need you to grow a Redwood tree for us?"));
		assertSame(ContractCrop.TORSTOL,
			ContractParser.parseAssignment("We need you to grow some Torstol for us?"));
	}

	@Test
	public void unrelatedDialogueYieldsNoContract()
	{
		assertNull(ContractParser.parseAssignment("Hello there, welcome to the Farming Guild."));
		assertNull(ContractParser.parseAssignment(""));
		assertNull(ContractParser.parseAssignment(null));
	}

	@Test
	public void unknownCropNameYieldsNoContract()
	{
		// Parses structurally but is not a crop we know, so we must not guess.
		assertNull(ContractParser.parseAssignment("We need you to grow some Banana bread for us?"));
	}

	@Test
	public void detectsTheRewardDialogue()
	{
		assertTrue(ContractParser.isContractRewarded(
			"You'll be wanting a reward then. Here you go."));
		assertFalse(ContractParser.isContractRewarded("Here you go."));
		assertFalse(ContractParser.isContractRewarded(null));
	}

	// -------------------------------------------------- 8. game-message parsing

	@Test
	public void detectsContractCancellation()
	{
		assertTrue(ContractParser.isContractCancelled(
			"You have cancelled your current farming contract."));
		assertFalse(ContractParser.isContractCancelled("You plant the maple sapling."));
		assertFalse(ContractParser.isContractCancelled(null));
	}
}
