package com.farmingcontract.data;

import com.farmingcontract.farming.Produce;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Every crop Guildmaster Jane can assign as a Farming Guild contract, with the patch it grows in,
 * the seed or sapling required, and the crop-specific gardener protection payment.
 *
 * <p>Each entry is keyed to a vendored {@link Produce} constant so that a contract parsed out of
 * Jane's dialogue (which yields a {@code Produce}) resolves straight to the data below.
 *
 * <p>Sources: patch types and produce identity from RuneLite's {@code Produce} enum; tier
 * membership, seed requirements and protection payments cross-checked against the OSRS Wiki
 * Farming contract and Seeds articles. Item IDs are {@link ItemID} gameval constants throughout —
 * no raw numbers.
 *
 * <p>Note that herb and flower crops <b>cannot be protected at any price</b>, and poison ivy
 * cannot become diseased, so those carry {@link ProtectionPayment#NONE}.
 */
@Getter
public enum ContractCrop
{
	// ---------------------------------------------------------------- Allotment (3 seeds)
	POTATO(Produce.POTATO, PatchType.ALLOTMENT, ItemID.POTATO_SEED, "Potato seed",
		ProtectionPayment.compost(2), EnumSet.of(ContractTier.EASY)),

	ONION(Produce.ONION, PatchType.ALLOTMENT, ItemID.ONION_SEED, "Onion seed",
		ProtectionPayment.sacks(ItemID.SACK_POTATO_10, "Sack of potatoes", 1,
			ItemID.POTATO, "Potatoes"),
		EnumSet.of(ContractTier.EASY)),

	CABBAGE(Produce.CABBAGE, PatchType.ALLOTMENT, ItemID.CABBAGE_SEED, "Cabbage seed",
		ProtectionPayment.sacks(ItemID.SACK_ONION_10, "Sack of onions", 1,
			ItemID.ONION, "Onions"),
		EnumSet.of(ContractTier.EASY)),

	TOMATO(Produce.TOMATO, PatchType.ALLOTMENT, ItemID.TOMATO_SEED, "Tomato seed",
		ProtectionPayment.sacks(ItemID.SACK_CABBAGE_10, "Sack of cabbages", 2,
			ItemID.CABBAGE, "Cabbages"),
		EnumSet.of(ContractTier.EASY)),

	SWEETCORN(Produce.SWEETCORN, PatchType.ALLOTMENT, ItemID.SWEETCORN_SEED, "Sweetcorn seed",
		ProtectionPayment.loose(ItemID.JUTE_FIBRE, "Jute fibre", 10),
		EnumSet.of(ContractTier.EASY)),

	STRAWBERRY(Produce.STRAWBERRY, PatchType.ALLOTMENT, ItemID.STRAWBERRY_SEED, "Strawberry seed",
		ProtectionPayment.baskets(ItemID.BASKET_APPLE_5, "Basket of apples", 1,
			ItemID.COOKING_APPLE, "Cooking apples"),
		EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM)),

	WATERMELON(Produce.WATERMELON, PatchType.ALLOTMENT, ItemID.WATERMELON_SEED, "Watermelon seed",
		ProtectionPayment.loose(ItemID.CURRY_LEAF, "Curry leaf", 10),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	SNAPE_GRASS(Produce.SNAPE_GRASS, PatchType.ALLOTMENT, ItemID.SNAPE_GRASS_SEED,
		"Snape grass seed",
		ProtectionPayment.loose(ItemID.JANGERBERRIES, "Jangerberries", 5),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	// ---------------------------------------------------------------- Flower (no protection)
	MARIGOLD(Produce.MARIGOLD, PatchType.FLOWER, ItemID.MARIGOLD_SEED, "Marigold seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	ROSEMARY(Produce.ROSEMARY, PatchType.FLOWER, ItemID.ROSEMARY_SEED, "Rosemary seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	NASTURTIUM(Produce.NASTURTIUM, PatchType.FLOWER, ItemID.NASTURTIUM_SEED, "Nasturtium seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	WOAD(Produce.WOAD, PatchType.FLOWER, ItemID.WOAD_SEED, "Woad seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	LIMPWURT(Produce.LIMPWURT, PatchType.FLOWER, ItemID.LIMPWURT_SEED, "Limpwurt seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	WHITE_LILY(Produce.WHITE_LILY, PatchType.FLOWER, ItemID.WHITE_LILY_SEED, "White lily seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	// ---------------------------------------------------------------- Bush
	REDBERRIES(Produce.REDBERRIES, PatchType.BUSH, ItemID.REDBERRY_BUSH_SEED, "Redberry seed",
		ProtectionPayment.sacks(ItemID.SACK_CABBAGE_10, "Sack of cabbages", 4,
			ItemID.CABBAGE, "Cabbages"),
		EnumSet.of(ContractTier.EASY)),

	CADAVABERRIES(Produce.CADAVABERRIES, PatchType.BUSH, ItemID.CADAVABERRY_BUSH_SEED,
		"Cadavaberry seed",
		ProtectionPayment.baskets(ItemID.BASKET_TOMATO_5, "Basket of tomatoes", 3,
			ItemID.TOMATO, "Tomatoes"),
		EnumSet.of(ContractTier.EASY)),

	DWELLBERRIES(Produce.DWELLBERRIES, PatchType.BUSH, ItemID.DWELLBERRY_BUSH_SEED,
		"Dwellberry seed",
		ProtectionPayment.baskets(ItemID.BASKET_STRAWBERRY_5, "Basket of strawberries", 3,
			ItemID.STRAWBERRY, "Strawberries"),
		EnumSet.of(ContractTier.EASY)),

	JANGERBERRIES(Produce.JANGERBERRIES, PatchType.BUSH, ItemID.JANGERBERRY_BUSH_SEED,
		"Jangerberry seed",
		ProtectionPayment.loose(ItemID.WATERMELON, "Watermelons", 6),
		EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM)),

	WHITEBERRIES(Produce.WHITEBERRIES, PatchType.BUSH, ItemID.WHITEBERRY_BUSH_SEED,
		"Whiteberry seed",
		ProtectionPayment.loose(ItemID.BITTERCAP_MUSHROOM, "Bittercap mushrooms", 8),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	POISON_IVY(Produce.POISON_IVY, PatchType.BUSH, ItemID.POISONIVY_BUSH_SEED, "Poison ivy seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	// ---------------------------------------------------------------- Herb (no protection)
	GUAM(Produce.GUAM, PatchType.HERB, ItemID.GUAM_SEED, "Guam seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	MARRENTILL(Produce.MARRENTILL, PatchType.HERB, ItemID.MARRENTILL_SEED, "Marrentill seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	TARROMIN(Produce.TARROMIN, PatchType.HERB, ItemID.TARROMIN_SEED, "Tarromin seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	HARRALANDER(Produce.HARRALANDER, PatchType.HERB, ItemID.HARRALANDER_SEED, "Harralander seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	RANARR(Produce.RANARR, PatchType.HERB, ItemID.RANARR_SEED, "Ranarr seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	TOADFLAX(Produce.TOADFLAX, PatchType.HERB, ItemID.TOADFLAX_SEED, "Toadflax seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY)),

	IRIT(Produce.IRIT, PatchType.HERB, ItemID.IRIT_SEED, "Irit seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM)),

	AVANTOE(Produce.AVANTOE, PatchType.HERB, ItemID.AVANTOE_SEED, "Avantoe seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM)),

	KWUARM(Produce.KWUARM, PatchType.HERB, ItemID.KWUARM_SEED, "Kwuarm seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.MEDIUM)),

	SNAPDRAGON(Produce.SNAPDRAGON, PatchType.HERB, ItemID.SNAPDRAGON_SEED, "Snapdragon seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	CADANTINE(Produce.CADANTINE, PatchType.HERB, ItemID.CADANTINE_SEED, "Cadantine seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	LANTADYME(Produce.LANTADYME, PatchType.HERB, ItemID.LANTADYME_SEED, "Lantadyme seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	DWARF_WEED(Produce.DWARF_WEED, PatchType.HERB, ItemID.DWARF_WEED_SEED, "Dwarf weed seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.HARD)),

	TORSTOL(Produce.TORSTOL, PatchType.HERB, ItemID.TORSTOL_SEED, "Torstol seed",
		ProtectionPayment.NONE, EnumSet.of(ContractTier.HARD)),

	// ---------------------------------------------------------------- Tree (sapling)
	OAK(Produce.OAK, PatchType.TREE, ItemID.PLANTPOT_OAK_SAPLING, "Oak sapling",
		ProtectionPayment.baskets(ItemID.BASKET_TOMATO_5, "Basket of tomatoes", 1,
			ItemID.TOMATO, "Tomatoes"),
		EnumSet.of(ContractTier.EASY)),

	WILLOW(Produce.WILLOW, PatchType.TREE, ItemID.PLANTPOT_WILLOW_SAPLING, "Willow sapling",
		ProtectionPayment.baskets(ItemID.BASKET_APPLE_5, "Basket of apples", 1,
			ItemID.COOKING_APPLE, "Cooking apples"),
		EnumSet.of(ContractTier.EASY)),

	MAPLE(Produce.MAPLE, PatchType.TREE, ItemID.PLANTPOT_MAPLE_SAPLING, "Maple sapling",
		ProtectionPayment.baskets(ItemID.BASKET_ORANGE_5, "Basket of oranges", 1,
			ItemID.ORANGE, "Oranges"),
		EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM, ContractTier.HARD)),

	YEW(Produce.YEW, PatchType.TREE, ItemID.PLANTPOT_YEW_SAPLING, "Yew sapling",
		ProtectionPayment.loose(ItemID.CACTUS_SPINE, "Cactus spines", 10),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	MAGIC(Produce.MAGIC, PatchType.TREE, ItemID.PLANTPOT_MAGIC_TREE_SAPLING, "Magic sapling",
		ProtectionPayment.loose(ItemID.COCONUT, "Coconuts", 25),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	// ---------------------------------------------------------------- Fruit tree (sapling)
	APPLE(Produce.APPLE, PatchType.FRUIT_TREE, ItemID.PLANTPOT_APPLE_SAPLING, "Apple sapling",
		ProtectionPayment.loose(ItemID.SWEETCORN, "Sweetcorn", 9),
		EnumSet.of(ContractTier.EASY)),

	BANANA(Produce.BANANA, PatchType.FRUIT_TREE, ItemID.PLANTPOT_BANANA_SAPLING, "Banana sapling",
		ProtectionPayment.baskets(ItemID.BASKET_APPLE_5, "Basket of apples", 4,
			ItemID.COOKING_APPLE, "Cooking apples"),
		EnumSet.of(ContractTier.EASY)),

	ORANGE(Produce.ORANGE, PatchType.FRUIT_TREE, ItemID.PLANTPOT_ORANGE_SAPLING, "Orange sapling",
		ProtectionPayment.baskets(ItemID.BASKET_STRAWBERRY_5, "Basket of strawberries", 3,
			ItemID.STRAWBERRY, "Strawberries"),
		EnumSet.of(ContractTier.EASY)),

	CURRY(Produce.CURRY, PatchType.FRUIT_TREE, ItemID.PLANTPOT_CURRY_SAPLING, "Curry sapling",
		ProtectionPayment.baskets(ItemID.BASKET_BANANA_5, "Basket of bananas", 5,
			ItemID.BANANA, "Bananas"),
		EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM)),

	PINEAPPLE(Produce.PINEAPPLE, PatchType.FRUIT_TREE, ItemID.PLANTPOT_PINEAPPLE_SAPLING,
		"Pineapple sapling",
		ProtectionPayment.loose(ItemID.WATERMELON, "Watermelons", 10),
		EnumSet.of(ContractTier.MEDIUM)),

	PAPAYA(Produce.PAPAYA, PatchType.FRUIT_TREE, ItemID.PLANTPOT_PAPAYA_SAPLING, "Papaya sapling",
		ProtectionPayment.loose(ItemID.PINEAPPLE, "Pineapples", 10),
		EnumSet.of(ContractTier.MEDIUM)),

	PALM(Produce.PALM, PatchType.FRUIT_TREE, ItemID.PLANTPOT_PALM_SAPLING, "Palm sapling",
		ProtectionPayment.loose(ItemID.PAPAYA, "Papaya fruit", 15),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	DRAGONFRUIT(Produce.DRAGONFRUIT, PatchType.FRUIT_TREE, ItemID.PLANTPOT_DRAGONFRUIT_SAPLING,
		"Dragonfruit sapling",
		ProtectionPayment.loose(ItemID.COCONUT, "Coconuts", 15),
		EnumSet.of(ContractTier.HARD)),

	// ---------------------------------------------------------------- Cactus
	CACTUS(Produce.CACTUS, PatchType.CACTUS, ItemID.CACTUS_SEED, "Cactus seed",
		ProtectionPayment.loose(ItemID.CADAVABERRIES, "Cadava berries", 6),
		EnumSet.of(ContractTier.EASY, ContractTier.MEDIUM)),

	POTATO_CACTUS(Produce.POTATO_CACTUS, PatchType.CACTUS, ItemID.POTATO_CACTUS_SEED,
		"Potato cactus seed",
		ProtectionPayment.loose(ItemID.SNAPE_GRASS, "Snape grass", 8),
		EnumSet.of(ContractTier.MEDIUM, ContractTier.HARD)),

	// ---------------------------------------------------------------- Celastrus / Redwood
	CELASTRUS(Produce.CELASTRUS, PatchType.CELASTRUS, ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING,
		"Celastrus sapling",
		ProtectionPayment.loose(ItemID.CACTUS_POTATO, "Potato cacti", 8),
		EnumSet.of(ContractTier.HARD)),

	REDWOOD(Produce.REDWOOD, PatchType.REDWOOD, ItemID.PLANTPOT_REDWOOD_TREE_SAPLING,
		"Redwood sapling",
		ProtectionPayment.loose(ItemID.DRAGONFRUIT, "Dragonfruit", 6),
		EnumSet.of(ContractTier.HARD));

	private static final Map<Produce, ContractCrop> BY_PRODUCE;
	private static final Map<String, ContractCrop> BY_CONTRACT_NAME;

	static
	{
		Map<Produce, ContractCrop> byProduce = new HashMap<>();
		Map<String, ContractCrop> byName = new HashMap<>();
		for (ContractCrop crop : values())
		{
			byProduce.put(crop.produce, crop);
			byName.put(crop.produce.getContractName().toLowerCase(), crop);
		}
		BY_PRODUCE = Collections.unmodifiableMap(byProduce);
		BY_CONTRACT_NAME = Collections.unmodifiableMap(byName);
	}

	private final Produce produce;

	private final PatchType patchType;

	/**
	 * The seed or sapling the player must plant. For trees, fruit trees, celastrus and redwood
	 * this is the <b>sapling</b>, not the raw seed.
	 */
	private final int seedItemId;

	private final String seedName;

	private final ProtectionPayment protection;

	private final Set<ContractTier> tiers;

	ContractCrop(Produce produce, PatchType patchType, int seedItemId, String seedName,
		ProtectionPayment protection, Set<ContractTier> tiers)
	{
		this.produce = produce;
		this.patchType = patchType;
		this.seedItemId = seedItemId;
		this.seedName = seedName;
		this.protection = protection;
		this.tiers = Collections.unmodifiableSet(tiers);
	}

	/**
	 * The name Jane uses for this contract, e.g. "Maple tree".
	 */
	public String getContractName()
	{
		return produce.getContractName();
	}

	/**
	 * How many seeds or saplings one planting needs.
	 */
	public int getSeedQuantity()
	{
		return patchType.getPlantQuantity();
	}

	/**
	 * The harvested produce item, used for the panel's crop icon.
	 */
	public int getProduceItemId()
	{
		return produce.getItemID();
	}

	@Nullable
	public static ContractCrop forProduce(@Nullable Produce produce)
	{
		return produce == null ? null : BY_PRODUCE.get(produce);
	}

	/**
	 * Resolve a contract name as spoken by Guildmaster Jane, case-insensitively.
	 */
	@Nullable
	public static ContractCrop forContractName(@Nullable String name)
	{
		return name == null ? null : BY_CONTRACT_NAME.get(name.trim().toLowerCase());
	}
}
