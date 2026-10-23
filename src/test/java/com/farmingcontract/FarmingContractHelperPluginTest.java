package com.farmingcontract;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Dev launcher: starts a RuneLite client with this plugin side-loaded, for manual in-game testing.
 * Run with {@code ./gradlew run}.
 */
public class FarmingContractHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FarmingContractHelperPlugin.class);
		RuneLite.main(args);
	}
}
