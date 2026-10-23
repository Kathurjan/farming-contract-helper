package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.farming.CropState;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * An immutable snapshot of everything the UI needs to render: the contract, where its patch stands,
 * and whether the required items are to hand.
 *
 * <p>Runtime state only — static crop facts live in {@link ContractCrop}.
 */
@Value
public class ContractState
{
	/**
	 * The state shown before we have ever seen a contract for this account.
	 */
	public static final ContractState UNKNOWN =
		new ContractState(null, null, -1, 0L, ItemAvailability.unknown(), ItemAvailability.unknown());

	/**
	 * Null when the contract could not be determined.
	 */
	@Nullable
	ContractCrop crop;

	/**
	 * Null when the patch has not been observed this session, or the crop is not planted.
	 */
	@Nullable
	CropState cropState;

	/**
	 * Growth stage within {@link #cropState}, or -1 when unknown.
	 */
	int stage;

	/**
	 * Epoch millis at which the crop finishes growing, or 0 when not estimable.
	 */
	long completionTime;

	ItemAvailability seed;

	ItemAvailability protection;

	public boolean hasContract()
	{
		return crop != null;
	}

	/**
	 * True when the patch has been read and holds nothing of the contract crop.
	 */
	public boolean isNotPlanted()
	{
		return cropState == null || cropState == CropState.EMPTY;
	}

	public String getPatchStatusText()
	{
		if (cropState == null)
		{
			return "Not tracked";
		}

		switch (cropState)
		{
			case EMPTY:
				return "Not planted";
			case GROWING:
				return "Growing";
			case HARVESTABLE:
				return "Ready";
			case DISEASED:
				return "Diseased";
			case DEAD:
				return "Dead";
			case FILLING:
				return "Filling";
			default:
				return cropState.name();
		}
	}
}
