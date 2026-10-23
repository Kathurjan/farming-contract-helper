package com.farmingcontract.data;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * A gardener protection payment for one specific crop.
 *
 * <p>The gardener requires payment <b>in the exact form requested</b>. Where that form is a
 * filled basket or sack, the loose produce is recorded separately via {@link #looseItemId} so the
 * plugin can tell the player how much raw produce a batch needs — but loose produce is
 * deliberately <b>not</b> treated as payable. See {@link #isContainerRequired()}.
 */
@Getter
@RequiredArgsConstructor
public class ProtectionPayment
{
	/**
	 * A crop that the gardener will not protect at any price (herbs and flowers), or that needs
	 * no protection because it cannot become diseased (poison ivy).
	 */
	public static final ProtectionPayment NONE = new ProtectionPayment(-1, "", 0, -1, "", 0);

	/**
	 * How many loose fruit fill one basket.
	 */
	public static final int BASKET_CAPACITY = 5;

	/**
	 * How many loose vegetables fill one sack.
	 */
	public static final int SACK_CAPACITY = 10;

	/**
	 * Item the gardener actually accepts.
	 */
	private final int itemId;

	private final String itemName;

	/**
	 * How many of {@link #itemId} the gardener wants.
	 */
	private final int quantity;

	/**
	 * When the payment is a filled basket or sack, the loose produce that fills it; otherwise -1.
	 */
	private final int looseItemId;

	private final String looseItemName;

	/**
	 * Total loose produce needed to fill {@link #quantity} containers; 0 when not a container.
	 */
	private final int looseQuantity;

	/**
	 * A payment made directly in loose items, e.g. 10 jute fibre.
	 */
	public static ProtectionPayment loose(int itemId, String itemName, int quantity)
	{
		return new ProtectionPayment(itemId, itemName, quantity, -1, "", 0);
	}

	/**
	 * A payment that must be handed over as filled baskets.
	 */
	public static ProtectionPayment baskets(int filledBasketId, String basketName, int baskets,
		int looseItemId, String looseItemName)
	{
		return new ProtectionPayment(filledBasketId, basketName, baskets,
			looseItemId, looseItemName, baskets * BASKET_CAPACITY);
	}

	/**
	 * A payment that must be handed over as filled sacks.
	 */
	public static ProtectionPayment sacks(int filledSackId, String sackName, int sacks,
		int looseItemId, String looseItemName)
	{
		return new ProtectionPayment(filledSackId, sackName, sacks,
			looseItemId, looseItemName, sacks * SACK_CAPACITY);
	}

	/**
	 * Two buckets of compost, the potato contract's payment.
	 */
	public static ProtectionPayment compost(int quantity)
	{
		return loose(ItemID.BUCKET_COMPOST, "Bucket of compost", quantity);
	}

	public boolean isAvailable()
	{
		return itemId != -1;
	}

	/**
	 * True when the gardener insists on a filled basket or sack, meaning loose produce in the
	 * inventory is <b>not</b> immediately payable.
	 */
	public boolean isContainerRequired()
	{
		return looseItemId != -1;
	}

	@Nullable
	public String getContentsDescription()
	{
		if (!isContainerRequired())
		{
			return null;
		}
		return looseQuantity + " " + looseItemName;
	}

	public String getDescription()
	{
		if (!isAvailable())
		{
			return "Not available";
		}
		return quantity + " " + itemName;
	}
}
