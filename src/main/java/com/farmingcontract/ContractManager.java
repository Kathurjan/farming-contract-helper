package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.data.PatchType;
import com.farmingcontract.farming.CropState;
import com.farmingcontract.farming.PatchState;
import com.farmingcontract.farming.Produce;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;

/**
 * Works out which farming contract the player has, keeps it persisted per account, and decodes the
 * state of the contract's patch.
 *
 * <p>Detection order, most to least reliable:
 * <ol>
 *     <li>Guildmaster Jane's dialogue, read live while the player talks to her. This is the only
 *     authoritative source in the game — there is no farming contract varbit.</li>
 *     <li>This plugin's own persisted value for the current RuneScape profile.</li>
 *     <li>RuneLite's built-in Time Tracking plugin's stored contract, read through the supported
 *     {@link ConfigManager} profile API. This is a plain config read, not a dependency on that
 *     plugin's classes, and it lets us start with the right contract if the player has used Time
 *     Tracking before installing this plugin.</li>
 * </ol>
 *
 * <p>A crop merely being planted in the Farming Guild is deliberately <b>never</b> treated as
 * evidence of a contract, because players pre-plant crops.
 */
@Slf4j
@Singleton
public class ContractManager
{
	static final String CONFIG_GROUP = "farmingcontracthelper";
	static final String KEY_CONTRACT = "contract";
	static final String KEY_NOTIFIED = "notifiedReady";
	static final String KEY_BANK_CACHE = "bankCache";
	static final String KEY_VAULT_CACHE = "seedVaultCache";
	static final String KEY_PATCH_OBSERVATION = "patchObservation";

	/**
	 * RuneLite's Time Tracking config group, read only for interoperability.
	 */
	private static final String TIMETRACKING_GROUP = "timetracking";
	private static final String TIMETRACKING_CONTRACT = "contract";
	private static final String FARM_TICK_OFFSET = "farmTickOffset";
	private static final String FARM_TICK_OFFSET_PRECISION = "farmTickOffsetPrecision";

	public static final int FARMING_GUILD_REGION_ID = 4922;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private InventoryChecker inventoryChecker;

	@Inject
	ContractManager()
	{
	}

	/**
	 * Test constructor. Reflection is forbidden in hub plugins, so tests wire dependencies here
	 * rather than poking at private fields.
	 */
	ContractManager(Client client, ConfigManager configManager, InventoryChecker inventoryChecker)
	{
		this.client = client;
		this.configManager = configManager;
		this.inventoryChecker = inventoryChecker;
	}

	@Getter
	@Nullable
	private ContractCrop contract;

	/**
	 * True once we have notified for the current planting, so we do not notify twice.
	 */
	@Getter
	private boolean notifiedReady;

	/**
	 * Last observed varbit value and the unix second we saw it, per patch varbit.
	 */
	private final Map<Integer, long[]> observations = new HashMap<>();

	// ------------------------------------------------------------------ detection

	/**
	 * Inspect Guildmaster Jane's dialogue. Returns true if the contract changed.
	 */
	public boolean checkJaneDialogue()
	{
		Widget head = client.getWidget(InterfaceID.ChatLeft.HEAD);
		if (head == null || head.getModelId() != NpcID.FARMING_GUILD_MASTER)
		{
			return false;
		}

		Widget textWidget = client.getWidget(InterfaceID.ChatLeft.TEXT);
		if (textWidget == null)
		{
			return false;
		}

		String dialog = Text.removeTags(textWidget.getText());

		if (ContractParser.isContractRewarded(dialog))
		{
			return setContract(null);
		}

		ContractCrop parsed = ContractParser.parseAssignment(dialog);
		if (parsed == null || parsed == contract)
		{
			return false;
		}

		return setContract(parsed);
	}

	/**
	 * Handle a game message that may cancel the contract. Returns true if the contract changed.
	 */
	public boolean checkGameMessage(String message)
	{
		if (ContractParser.isContractCancelled(Text.removeTags(message)))
		{
			return setContract(null);
		}
		return false;
	}

	/**
	 * Load the contract for the active profile, falling back to RuneLite's Time Tracking value.
	 */
	public void loadFromConfig()
	{
		ContractCrop own = readOwnContract();
		if (own != null)
		{
			contract = own;
		}
		else
		{
			contract = readTimeTrackingContract();
			if (contract != null)
			{
				log.debug("Seeded contract {} from Time Tracking config", contract);
				writeOwnContract(contract);
			}
		}

		Boolean notified = configManager.getRSProfileConfiguration(CONFIG_GROUP, KEY_NOTIFIED,
			Boolean.class);
		notifiedReady = Boolean.TRUE.equals(notified);

		inventoryChecker.restoreCaches(readCounts(KEY_BANK_CACHE), readCounts(KEY_VAULT_CACHE));
		readObservations();
	}

	@Nullable
	private ContractCrop readOwnContract()
	{
		String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, KEY_CONTRACT);
		if (stored == null || stored.isEmpty())
		{
			return null;
		}
		try
		{
			return ContractCrop.valueOf(stored);
		}
		catch (IllegalArgumentException e)
		{
			log.debug("Discarding unrecognised stored contract {}", stored);
			return null;
		}
	}

	/**
	 * Read the contract the built-in Time Tracking plugin persisted, which is stored as the
	 * produce item id.
	 */
	@Nullable
	private ContractCrop readTimeTrackingContract()
	{
		String stored = configManager.getRSProfileConfiguration(TIMETRACKING_GROUP,
			TIMETRACKING_CONTRACT);
		if (stored == null || stored.isEmpty())
		{
			return null;
		}
		try
		{
			Produce produce = Produce.getByItemID(Integer.parseInt(stored));
			return ContractCrop.forProduce(produce);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private void writeOwnContract(@Nullable ContractCrop crop)
	{
		if (crop == null)
		{
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP, KEY_CONTRACT);
		}
		else
		{
			configManager.setRSProfileConfiguration(CONFIG_GROUP, KEY_CONTRACT, crop.name());
		}
	}

	/**
	 * Set the contract, persist it, and reset notification state. Returns true if it changed.
	 */
	public boolean setContract(@Nullable ContractCrop crop)
	{
		if (crop == contract)
		{
			return false;
		}

		contract = crop;
		writeOwnContract(crop);
		resetNotified();
		return true;
	}

	// ------------------------------------------------------------------ notifications

	public void resetNotified()
	{
		notifiedReady = false;
		configManager.unsetRSProfileConfiguration(CONFIG_GROUP, KEY_NOTIFIED);
	}

	public void markNotified()
	{
		notifiedReady = true;
		configManager.setRSProfileConfiguration(CONFIG_GROUP, KEY_NOTIFIED, true);
	}

	// ------------------------------------------------------------------ patch state

	/**
	 * Record the current value of every patch varbit relevant to the contract. Called on varbit
	 * changes and on login. Returns true if a tracked patch value changed.
	 */
	public boolean observePatches()
	{
		if (contract == null)
		{
			return false;
		}

		boolean changed = false;
		long now = Instant.now().getEpochSecond();

		for (int varbit : contract.getPatchType().getVarbitList())
		{
			int value = client.getVarbitValue(varbit);
			long[] previous = observations.get(varbit);
			if (previous == null || previous[0] != value)
			{
				observations.put(varbit, new long[]{value, now});
				changed = true;
			}
		}

		if (changed)
		{
			writeObservations();
		}
		return changed;
	}

	/**
	 * Decode the contract patch, choosing the patch that actually holds the contract crop when the
	 * patch type is duplicated (the guild's two allotments).
	 */
	@Nullable
	public PatchState getContractPatchState()
	{
		if (contract == null)
		{
			return null;
		}

		PatchType type = contract.getPatchType();
		PatchState fallback = null;

		for (int varbit : type.getVarbitList())
		{
			long[] observation = observations.get(varbit);
			if (observation == null)
			{
				continue;
			}

			PatchState state = type.getImplementation().forVarbitValue((int) observation[0]);
			if (state == null)
			{
				continue;
			}

			if (state.getProduce() == contract.getProduce())
			{
				return state;
			}

			if (fallback == null)
			{
				fallback = state;
			}
		}

		return fallback;
	}

	/**
	 * The unix second at which the patch state used by {@link #getContractPatchState()} was seen.
	 */
	private long getObservationTime()
	{
		if (contract == null)
		{
			return 0L;
		}

		long best = 0L;
		for (int varbit : contract.getPatchType().getVarbitList())
		{
			long[] observation = observations.get(varbit);
			if (observation != null && observation[1] > best)
			{
				best = observation[1];
			}
		}
		return best;
	}

	/**
	 * Build the immutable snapshot the UI renders.
	 */
	public ContractState buildState(boolean checkBank, boolean checkVault)
	{
		if (contract == null)
		{
			return ContractState.UNKNOWN;
		}

		PatchState patch = getContractPatchState();
		ItemAvailability seed = inventoryChecker.checkSeed(contract, checkBank, checkVault);
		ItemAvailability protection = inventoryChecker.checkProtection(contract, checkBank,
			checkVault);

		if (patch == null)
		{
			return new ContractState(contract, null, -1, 0L, seed, protection);
		}

		Integer offset = configManager.getRSProfileConfiguration(TIMETRACKING_GROUP,
			FARM_TICK_OFFSET, Integer.class);
		Integer precision = configManager.getRSProfileConfiguration(TIMETRACKING_GROUP,
			FARM_TICK_OFFSET_PRECISION, Integer.class);

		long observed = getObservationTime();
		int tickRate = patch.getTickRate();
		int stages = patch.getStages();
		int stage = FarmTick.currentStage(observed, Instant.now().getEpochSecond(),
			patch.getStage(), stages, tickRate, offset, precision);
		long completion = FarmTick.completionTime(observed, stage, stages, tickRate, offset,
			precision);

		CropState cropState = patch.getCropState();

		// An empty patch, or one holding something other than the contract crop, is "not planted"
		// as far as the contract is concerned. We never infer a contract from what is planted.
		if (patch.getProduce() != contract.getProduce())
		{
			cropState = CropState.EMPTY;
			completion = 0L;
			stage = -1;
		}

		return new ContractState(contract, cropState, stage, completion, seed, protection);
	}

	// ------------------------------------------------------------------ persistence helpers

	private void writeObservations()
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, long[]> entry : observations.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(entry.getKey()).append(':').append(entry.getValue()[0]).append(':')
				.append(entry.getValue()[1]);
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, KEY_PATCH_OBSERVATION, sb.toString());
	}

	private void readObservations()
	{
		observations.clear();
		String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP,
			KEY_PATCH_OBSERVATION);
		if (stored == null || stored.isEmpty())
		{
			return;
		}

		for (String entry : stored.split(","))
		{
			String[] parts = entry.split(":");
			if (parts.length != 3)
			{
				continue;
			}
			try
			{
				observations.put(Integer.parseInt(parts[0]),
					new long[]{Long.parseLong(parts[1]), Long.parseLong(parts[2])});
			}
			catch (NumberFormatException ignored)
			{
				// skip malformed entry
			}
		}
	}

	/**
	 * Persist the last seen bank and seed vault counts so they survive a relog.
	 */
	public void persistCaches()
	{
		writeCounts(KEY_BANK_CACHE, inventoryChecker.getBankCache());
		writeCounts(KEY_VAULT_CACHE, inventoryChecker.getSeedVaultCache());
	}

	private void writeCounts(String key, Map<Integer, Integer> counts)
	{
		if (counts.isEmpty())
		{
			return;
		}

		// Only the items this plugin could ever ask about are worth persisting.
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> entry : counts.entrySet())
		{
			if (!RELEVANT_ITEMS.contains(entry.getKey()))
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(entry.getKey()).append(':').append(entry.getValue());
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, key, sb.toString());
	}

	private Map<Integer, Integer> readCounts(String key)
	{
		Map<Integer, Integer> out = new HashMap<>();
		String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
		if (stored == null || stored.isEmpty())
		{
			return out;
		}

		for (String entry : stored.split(","))
		{
			String[] parts = entry.split(":");
			if (parts.length != 2)
			{
				continue;
			}
			try
			{
				out.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			}
			catch (NumberFormatException ignored)
			{
				// skip malformed entry
			}
		}
		return out;
	}

	/**
	 * Every item this plugin can ask about, so persisted caches stay small.
	 */
	private static final java.util.Set<Integer> RELEVANT_ITEMS = buildRelevantItems();

	private static java.util.Set<Integer> buildRelevantItems()
	{
		java.util.Set<Integer> items = new java.util.HashSet<>();
		for (ContractCrop crop : ContractCrop.values())
		{
			items.add(crop.getSeedItemId());
			if (crop.getProtection().isAvailable())
			{
				items.add(crop.getProtection().getItemId());
				if (crop.getProtection().isContainerRequired())
				{
					items.add(crop.getProtection().getLooseItemId());
				}
			}
		}
		return items;
	}

	/**
	 * Wipe volatile state when the profile changes so one account never shows another's data.
	 */
	public void onProfileChanged()
	{
		contract = null;
		notifiedReady = false;
		observations.clear();
		inventoryChecker.reset();
		loadFromConfig();
	}

	public boolean isInFarmingGuild()
	{
		net.runelite.api.WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return false;
		}

		int[] mapRegions = worldView.getMapRegions();
		if (mapRegions == null)
		{
			return false;
		}
		for (int region : mapRegions)
		{
			if (region == FARMING_GUILD_REGION_ID)
			{
				return true;
			}
		}
		return false;
	}
}
