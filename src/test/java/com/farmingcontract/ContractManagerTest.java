package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Persisted contract loading and account switching, driven through a fake profile config store.
 * Spec test categories 12 and 13.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ContractManagerTest
{
	@Mock
	private Client client;

	@Mock
	private ConfigManager configManager;

	@Mock
	private ItemManager itemManager;

	private ContractManager manager;

	/**
	 * Stands in for RuneLite's per-RS-profile config store.
	 */
	private final Map<String, String> profileConfig = new HashMap<>();

	@Before
	public void setUp()
	{
		manager = new ContractManager(client, configManager, new InventoryChecker(client, itemManager));

		when(configManager.getRSProfileConfiguration(anyString(), anyString()))
			.thenAnswer(i -> profileConfig.get(key(i.getArgument(0), i.getArgument(1))));

		// The typed overload, used for the notified flag and the farm tick offset.
		when(configManager.getRSProfileConfiguration(anyString(), anyString(), any(Class.class)))
			.thenAnswer(i ->
			{
				String raw = profileConfig.get(key(i.getArgument(0), i.getArgument(1)));
				if (raw == null)
				{
					return null;
				}
				Class<?> type = i.getArgument(2);
				if (type == Boolean.class)
				{
					return Boolean.valueOf(raw);
				}
				if (type == Integer.class)
				{
					try
					{
						return Integer.valueOf(raw);
					}
					catch (NumberFormatException e)
					{
						return null;
					}
				}
				return raw;
			});

		doAnswer(i ->
		{
			// Cast to Object: String.valueOf is overloaded and would otherwise resolve to the
			// char[] variant against Mockito's generic getArgument.
			profileConfig.put(key(i.getArgument(0), i.getArgument(1)),
				String.valueOf((Object) i.getArgument(2)));
			return null;
		}).when(configManager).setRSProfileConfiguration(anyString(), anyString(), any());

		doAnswer(i ->
		{
			profileConfig.remove(key(i.getArgument(0), i.getArgument(1)));
			return null;
		}).when(configManager).unsetRSProfileConfiguration(anyString(), anyString());
	}

	private static String key(String group, String name)
	{
		return group + ":" + name;
	}

	// -------------------------------------------------- 12. persisted contract loading

	@Test
	public void loadsItsOwnPersistedContract()
	{
		profileConfig.put(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT), "MAPLE");

		manager.loadFromConfig();

		assertSame(ContractCrop.MAPLE, manager.getContract());
	}

	@Test
	public void unrecognisedPersistedValueIsDiscardedNotCrashed()
	{
		profileConfig.put(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT),
			"SPACE_POTATO");

		manager.loadFromConfig();

		assertNull(manager.getContract());
	}

	@Test
	public void seedsFromTimeTrackingWhenWeHaveNothingStored()
	{
		// RuneLite's Time Tracking plugin stores the contract as the produce item id.
		profileConfig.put(key("timetracking", "contract"), String.valueOf(ItemID.MAPLE_LOGS));

		manager.loadFromConfig();

		assertSame(ContractCrop.MAPLE, manager.getContract());
		// And it is adopted as our own so we no longer depend on that plugin being enabled.
		assertSame("MAPLE",
			profileConfig.get(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT)));
	}

	@Test
	public void ownValueWinsOverTimeTracking()
	{
		profileConfig.put(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT), "TORSTOL");
		profileConfig.put(key("timetracking", "contract"), String.valueOf(ItemID.MAPLE_LOGS));

		manager.loadFromConfig();

		assertSame(ContractCrop.TORSTOL, manager.getContract());
	}

	@Test
	public void malformedTimeTrackingValueIsIgnored()
	{
		profileConfig.put(key("timetracking", "contract"), "not-a-number");

		manager.loadFromConfig();

		assertNull(manager.getContract());
	}

	@Test
	public void settingAContractPersistsItAndClearingRemovesIt()
	{
		assertTrue(manager.setContract(ContractCrop.YEW));
		assertSame(ContractCrop.YEW, manager.getContract());
		assertSame("YEW",
			profileConfig.get(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT)));

		assertTrue(manager.setContract(null));
		assertNull(manager.getContract());
		assertFalse(profileConfig.containsKey(
			key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT)));
	}

	@Test
	public void settingTheSameContractIsNotAChange()
	{
		manager.setContract(ContractCrop.YEW);
		assertFalse(manager.setContract(ContractCrop.YEW));
	}

	@Test
	public void changingContractResetsTheNotificationFlag()
	{
		manager.setContract(ContractCrop.YEW);
		manager.markNotified();
		assertTrue(manager.isNotifiedReady());

		manager.setContract(ContractCrop.MAPLE);
		assertFalse(manager.isNotifiedReady());
	}

	// -------------------------------------------------- 13. account switching

	@Test
	public void switchingProfileReloadsForTheNewAccount()
	{
		profileConfig.put(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT), "MAPLE");
		manager.loadFromConfig();
		assertSame(ContractCrop.MAPLE, manager.getContract());

		// Second account has a different contract stored.
		profileConfig.put(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT), "TORSTOL");
		manager.onProfileChanged();

		assertSame(ContractCrop.TORSTOL, manager.getContract());
	}

	@Test
	public void switchingToAnAccountWithNoContractClearsTheOldOne()
	{
		profileConfig.put(key(ContractManager.CONFIG_GROUP, ContractManager.KEY_CONTRACT), "MAPLE");
		manager.loadFromConfig();
		assertSame(ContractCrop.MAPLE, manager.getContract());

		profileConfig.clear();
		manager.onProfileChanged();

		assertNull("one account's contract must not leak into another", manager.getContract());
	}

	@Test
	public void notificationFlagIsPersistedPerProfile()
	{
		manager.setContract(ContractCrop.MAPLE);
		manager.markNotified();
		assertTrue(manager.isNotifiedReady());

		// Reload as if logging back in on the same account.
		manager.loadFromConfig();
		assertTrue(manager.isNotifiedReady());
	}
}
