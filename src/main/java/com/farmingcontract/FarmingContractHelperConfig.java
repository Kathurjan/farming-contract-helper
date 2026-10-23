package com.farmingcontract;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Notification;

@ConfigGroup(FarmingContractHelperConfig.GROUP)
public interface FarmingContractHelperConfig extends Config
{
	String GROUP = "farmingcontracthelper";

	@ConfigItem(
		keyName = "checkInventory",
		name = "Check inventory",
		description = "Show whether the seed and protection payment are in your inventory.",
		position = 1
	)
	default boolean checkInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "checkBank",
		name = "Check bank",
		description = "Also count items seen in your bank. Only accurate once you have opened your "
			+ "bank; until then the bank is reported as unknown rather than empty.",
		position = 2
	)
	default boolean checkBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "checkSeedVault",
		name = "Check seed vault",
		description = "Also count seeds seen in your Seed Vault. Only accurate once you have "
			+ "opened the vault.",
		position = 3
	)
	default boolean checkSeedVault()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showProtection",
		name = "Show protection payment",
		description = "Show the gardener protection payment for the contract crop.",
		position = 4
	)
	default boolean showProtection()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show compact overlay",
		description = "Draw a compact summary of the contract on the game screen.",
		position = 5
	)
	default boolean showOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightPatch",
		name = "Highlight relevant Farming Guild patch",
		description = "While in the Farming Guild, outline only the patch your contract crop "
			+ "belongs in.",
		position = 6
	)
	default boolean highlightPatch()
	{
		return false;
	}

	@ConfigItem(
		keyName = "readyNotification",
		name = "Show completion notification",
		description = "Notify once when the contract crop becomes ready to check.",
		position = 7
	)
	default Notification readyNotification()
	{
		return Notification.OFF;
	}
}
