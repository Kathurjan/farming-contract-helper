package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Pure parsing of Guildmaster Jane's dialogue and the farming-contract game messages.
 *
 * <p>Deliberately free of any client dependency so every branch is unit-testable. The plugin feeds
 * it already-detagged strings.
 *
 * <p>The dialogue wording is the same set RuneLite's own Time Tracking plugin recognises.
 */
public final class ContractParser
{
	/**
	 * Jane offering or restating the current contract, e.g.
	 * "We need you to grow a Maple tree for us?" / "Please could you grow some Guam for us?".
	 */
	private static final Pattern CONTRACT_ASSIGN = Pattern.compile(
		"(?:We need you to grow|Please could you grow) (?:some|a|an) ([a-zA-Z ]+)(?: for us\\?|\\.)");

	/**
	 * Jane handing over the reward, which clears the contract.
	 */
	private static final String CONTRACT_REWARDED = "You'll be wanting a reward then. Here you go.";

	/**
	 * Chat message shown when the player cancels or swaps their contract.
	 */
	private static final Pattern CONTRACT_CANCELLED = Pattern.compile(
		"You have cancelled your current farming contract");

	private ContractParser()
	{
	}

	/**
	 * Extract the crop Jane is asking for, or null if this dialogue is not a contract assignment.
	 */
	@Nullable
	public static ContractCrop parseAssignment(@Nullable String dialogText)
	{
		if (dialogText == null)
		{
			return null;
		}

		Matcher matcher = CONTRACT_ASSIGN.matcher(dialogText);
		if (!matcher.find())
		{
			return null;
		}

		return ContractCrop.forContractName(matcher.group(1));
	}

	/**
	 * True when this dialogue is Jane paying out, meaning the contract is finished.
	 */
	public static boolean isContractRewarded(@Nullable String dialogText)
	{
		return dialogText != null && dialogText.equals(CONTRACT_REWARDED);
	}

	/**
	 * True when this game message indicates the contract was cancelled or replaced.
	 */
	public static boolean isContractCancelled(@Nullable String message)
	{
		return message != null && CONTRACT_CANCELLED.matcher(message).find();
	}
}
