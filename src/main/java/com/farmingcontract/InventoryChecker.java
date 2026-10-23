package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.data.ProtectionPayment;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

/**
 * Reads item counts out of the player's containers. Read-only: this class never moves, uses or
 * withdraws anything.
 *
 * <p>The inventory is read live. Bank and Seed Vault contents are only visible to the client once
 * the player has opened them, so the last seen counts are cached here (and persisted by
 * {@link ContractManager} so they survive a relog). Cached counts are reported as "known"
 * quantities and are never used to claim an item is missing.
 */
@Singleton
public class InventoryChecker
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	InventoryChecker()
	{
	}

	/**
	 * Test constructor; see {@link ContractManager}.
	 */
	InventoryChecker(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	/**
	 * Last seen bank contents, item id to quantity. Empty until the bank is opened.
	 */
	private final Map<Integer, Integer> bankCache = new HashMap<>();

	/**
	 * Last seen seed vault contents. Empty until the vault is opened.
	 */
	private final Map<Integer, Integer> seedVaultCache = new HashMap<>();

	private boolean bankSeen;

	private boolean seedVaultSeen;

	/**
	 * Refresh the cache for whichever container just changed. Returns true if anything we track
	 * was updated.
	 */
	public boolean onContainerChanged(int containerId, @Nullable ItemContainer container)
	{
		if (container == null)
		{
			return false;
		}

		if (containerId == InventoryID.BANK)
		{
			snapshot(container, bankCache);
			bankSeen = true;
			return true;
		}

		if (containerId == InventoryID.SEED_VAULT)
		{
			snapshot(container, seedVaultCache);
			seedVaultSeen = true;
			return true;
		}

		return containerId == InventoryID.INV;
	}

	/**
	 * Snapshot a container, folding noted stacks into their un-noted item id. Bank and vault items
	 * are frequently noted, and for our purposes they are the same item.
	 */
	private void snapshot(ItemContainer container, Map<Integer, Integer> into)
	{
		into.clear();
		for (Item item : container.getItems())
		{
			if (item.getId() != -1)
			{
				into.merge(unnotedId(item.getId()), item.getQuantity(), Integer::sum);
			}
		}
	}

	/**
	 * The un-noted id for an item, or the id itself if it is not a note.
	 */
	private int unnotedId(int itemId)
	{
		ItemComposition composition = itemManager.getItemComposition(itemId);
		return composition.getNote() != -1 ? composition.getLinkedNoteId() : itemId;
	}

	/**
	 * True when this item id is the noted form of something.
	 */
	private boolean isNoted(int itemId)
	{
		return itemManager.getItemComposition(itemId).getNote() != -1;
	}

	/**
	 * Seed the caches from persisted values after a login, so counts are not blank until the
	 * player next opens the bank.
	 */
	public void restoreCaches(Map<Integer, Integer> bank, Map<Integer, Integer> seedVault)
	{
		if (!bank.isEmpty())
		{
			bankCache.clear();
			bankCache.putAll(bank);
			bankSeen = true;
		}
		if (!seedVault.isEmpty())
		{
			seedVaultCache.clear();
			seedVaultCache.putAll(seedVault);
			seedVaultSeen = true;
		}
	}

	public Map<Integer, Integer> getBankCache()
	{
		return new HashMap<>(bankCache);
	}

	public Map<Integer, Integer> getSeedVaultCache()
	{
		return new HashMap<>(seedVaultCache);
	}

	/**
	 * Forget everything. Called when the profile changes so one account's bank never shows up
	 * against another's.
	 */
	public void reset()
	{
		bankCache.clear();
		seedVaultCache.clear();
		bankSeen = false;
		seedVaultSeen = false;
	}

	/**
	 * Count an item in the inventory, returning {un-noted, noted}.
	 */
	private int[] inventoryCounts(int itemId)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return new int[]{0, 0};
		}

		int plain = 0;
		int noted = 0;
		for (Item item : inventory.getItems())
		{
			int id = item.getId();
			if (id == -1)
			{
				continue;
			}
			if (unnotedId(id) != itemId)
			{
				continue;
			}
			if (isNoted(id))
			{
				noted += item.getQuantity();
			}
			else
			{
				plain += item.getQuantity();
			}
		}
		return new int[]{plain, noted};
	}

	/**
	 * Availability of the seed or sapling for this contract.
	 */
	public ItemAvailability checkSeed(ContractCrop crop, boolean checkBank, boolean checkVault)
	{
		// A noted seed or sapling cannot be planted, so notes do not satisfy this requirement.
		return build(crop.getSeedItemId(), crop.getSeedQuantity(), checkBank, checkVault, false);
	}

	/**
	 * Availability of the protection payment, in the form the gardener actually accepts.
	 *
	 * <p>For basket and sack payments only the <b>filled container</b> counts towards being
	 * payable. Loose produce is reported separately by {@link #checkLooseProduce} so the UI can
	 * tell the player they still need to fill baskets, without falsely marking them ready to pay.
	 */
	public ItemAvailability checkProtection(ContractCrop crop, boolean checkBank, boolean checkVault)
	{
		ProtectionPayment payment = crop.getProtection();
		if (!payment.isAvailable())
		{
			return ItemAvailability.of(0, 0, 0, -1, -1, false);
		}
		// Gardeners accept protection payments in noted form.
		return build(payment.getItemId(), payment.getQuantity(), checkBank, checkVault, true);
	}

	/**
	 * Availability of the loose produce that fills the payment's baskets or sacks. Only meaningful
	 * when {@link ProtectionPayment#isContainerRequired()}.
	 */
	@Nullable
	public ItemAvailability checkLooseProduce(ContractCrop crop, boolean checkBank,
		boolean checkVault)
	{
		ProtectionPayment payment = crop.getProtection();
		if (!payment.isAvailable() || !payment.isContainerRequired())
		{
			return null;
		}
		return build(payment.getLooseItemId(), payment.getLooseQuantity(), checkBank, checkVault,
			true);
	}

	private ItemAvailability build(int itemId, int required, boolean checkBank, boolean checkVault,
		boolean notedAccepted)
	{
		int bank = checkBank && bankSeen ? bankCache.getOrDefault(itemId, 0) : -1;
		int vault = checkVault && seedVaultSeen ? seedVaultCache.getOrDefault(itemId, 0) : -1;
		int[] counts = inventoryCounts(itemId);
		return ItemAvailability.of(required, counts[0], counts[1], bank, vault, notedAccepted);
	}
}
