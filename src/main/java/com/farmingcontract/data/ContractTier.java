package com.farmingcontract.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Farming Guild contract difficulty tiers, as offered by Guildmaster Jane.
 * A crop may be offered by more than one tier.
 */
@Getter
@RequiredArgsConstructor
public enum ContractTier
{
	EASY("Easy", 45),
	MEDIUM("Medium", 65),
	HARD("Hard", 85);

	private final String displayName;

	/**
	 * Farming level at which this tier of contract becomes available.
	 */
	private final int farmingLevel;
}
