package com.farmingcontract;

import lombok.Value;

/**
 * How much of a required item the player has, where it is, and how confident we are about it.
 *
 * <p>Inventory counts are authoritative: the inventory is always loaded while logged in, so zero
 * there really is zero. Bank and Seed Vault counts are only known once the player has opened them,
 * so those are reported as "known" quantities and are never used to assert that an item is
 * <b>absent</b>. {@link #isSourcesLoaded()} separates the two cases so the UI can say "unknown"
 * rather than lie.
 *
 * <p>Noted items are counted separately. Gardeners accept protection payments in noted form, so for
 * payments a note is as good as the item ({@link #notedAccepted}). Seeds and saplings must be
 * un-noted before they can be planted, so there a note is reported but does not satisfy the
 * requirement.
 */
@Value
public class ItemAvailability
{
	/**
	 * What to show the player, in decreasing order of readiness.
	 */
	public enum Status
	{
		/**
		 * Nothing needed, e.g. a crop that cannot be protected.
		 */
		NOT_REQUIRED,

		/**
		 * Enough in the inventory right now.
		 */
		HAVE,

		/**
		 * Not carrying enough, but a note in the inventory covers it and the note is usable here.
		 */
		HAVE_NOTED,

		/**
		 * Not carrying it, but we have seen enough in the bank or Seed Vault.
		 */
		IN_STORAGE,

		/**
		 * Short in the inventory, and a container we would need to check has never been opened.
		 */
		UNKNOWN,

		/**
		 * Short everywhere we can see, and we have seen everywhere.
		 */
		MISSING
	}

	int required;

	/**
	 * Un-noted count in the inventory.
	 */
	int inInventory;

	/**
	 * Noted count in the inventory.
	 */
	int notedInInventory;

	/**
	 * Count seen in the bank, or -1 if the bank has never been opened.
	 */
	int inBank;

	/**
	 * Count seen in the seed vault, or -1 if the vault has never been opened.
	 */
	int inSeedVault;

	/**
	 * Whether a noted item satisfies this requirement. True for gardener payments, false for seeds
	 * and saplings, which must be un-noted before planting.
	 */
	boolean notedAccepted;

	public static ItemAvailability unknown()
	{
		return new ItemAvailability(0, 0, 0, -1, -1, false);
	}

	public static ItemAvailability of(int required, int inInventory, int notedInInventory,
		int inBank, int inSeedVault, boolean notedAccepted)
	{
		return new ItemAvailability(required, inInventory, notedInInventory, inBank, inSeedVault,
			notedAccepted);
	}

	public boolean isNotRequired()
	{
		return required <= 0;
	}

	public boolean isBankKnown()
	{
		return inBank >= 0;
	}

	public boolean isSeedVaultKnown()
	{
		return inSeedVault >= 0;
	}

	/**
	 * Usable right now from the inventory. Counts notes only where notes are accepted.
	 */
	public boolean isSatisfiedFromInventory()
	{
		return getUsableInInventory() >= required;
	}

	/**
	 * Inventory count that actually satisfies the requirement.
	 */
	public int getUsableInInventory()
	{
		return notedAccepted ? inInventory + notedInInventory : inInventory;
	}

	/**
	 * True when a note is what is covering the requirement, rather than the item itself.
	 */
	public boolean isCoveredByNote()
	{
		return isSatisfiedFromInventory() && inInventory < required && notedInInventory > 0;
	}

	/**
	 * True when the player holds a note that does not count, e.g. a noted sapling.
	 */
	public boolean hasUnusableNote()
	{
		return !notedAccepted && notedInInventory > 0 && inInventory < required;
	}

	public int getStorageTotal()
	{
		return Math.max(inBank, 0) + Math.max(inSeedVault, 0);
	}

	/**
	 * Inventory plus any container we have actually seen. Unopened containers contribute nothing.
	 */
	public int getKnownTotal()
	{
		return getUsableInInventory() + getStorageTotal();
	}

	public boolean isSatisfiedFromKnownSources()
	{
		return getKnownTotal() >= required;
	}

	/**
	 * True when every source we could check has been loaded, so a shortfall is a real shortfall.
	 */
	public boolean isSourcesLoaded()
	{
		return isBankKnown() && isSeedVaultKnown();
	}

	/**
	 * True only when we are certain the player does not have enough anywhere we can see.
	 */
	public boolean isDefinitelyShort()
	{
		return !isSatisfiedFromKnownSources() && isSourcesLoaded();
	}

	/**
	 * The single source of truth for how this requirement is presented.
	 */
	public Status getStatus()
	{
		if (isNotRequired())
		{
			return Status.NOT_REQUIRED;
		}
		if (inInventory >= required)
		{
			return Status.HAVE;
		}
		if (isSatisfiedFromInventory())
		{
			return Status.HAVE_NOTED;
		}
		if (isSatisfiedFromKnownSources())
		{
			return Status.IN_STORAGE;
		}
		return isSourcesLoaded() ? Status.MISSING : Status.UNKNOWN;
	}

	/**
	 * "2 / 5" style progress against the requirement, counting what is usable in the inventory.
	 */
	public String getInventoryProgress()
	{
		return getUsableInInventory() + " / " + required;
	}

	/**
	 * Where the shortfall can be made up from, e.g. "bank: 25", or null when nowhere known.
	 */
	public String getStorageDescription()
	{
		StringBuilder sb = new StringBuilder();
		if (isBankKnown() && inBank > 0)
		{
			sb.append("bank: ").append(inBank);
		}
		if (isSeedVaultKnown() && inSeedVault > 0)
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append("vault: ").append(inSeedVault);
		}
		return sb.length() == 0 ? null : sb.toString();
	}
}
