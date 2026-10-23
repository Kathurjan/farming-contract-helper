package com.farmingcontract;

/**
 * Farming tick arithmetic, kept pure so it can be unit-tested without a client.
 *
 * <p>Farming growth happens on global ticks whose length depends on the crop (5 minutes for
 * allotments, 40 for fruit trees, and so on). Ticks are aligned to a boundary that is <b>not</b>
 * necessarily the unix epoch boundary; the real world has an offset which RuneLite's Time Tracking
 * plugin learns by watching a patch change. When that learned offset is available we use it, which
 * makes the estimate exact; without it the estimate is aligned to the raw epoch boundary and can be
 * up to one tick early or late.
 *
 * <p>Ported from the tick arithmetic in RuneLite's {@code FarmingTracker} (BSD-2-Clause,
 * Copyright (c) 2018 Abex).
 */
public final class FarmTick
{
	/**
	 * The offset is only trustworthy if it was learned at at least this granularity.
	 */
	private static final int MIN_TRUSTWORTHY_PRECISION_MINS = 40;

	private FarmTick()
	{
	}

	/**
	 * Whether a learned offset can be applied to a crop with the given tick rate.
	 */
	public static boolean isOffsetUsable(Integer offsetPrecisionMins, Integer offsetMins, int tickRateMins)
	{
		return offsetPrecisionMins != null
			&& offsetMins != null
			&& (offsetPrecisionMins >= tickRateMins
				|| offsetPrecisionMins >= MIN_TRUSTWORTHY_PRECISION_MINS);
	}

	/**
	 * The unix time, in seconds, at which the tick {@code ticks} ahead of {@code requestedTime}
	 * will occur.
	 *
	 * @param tickRateMins minutes per growth tick for this crop
	 * @param ticks        how many ticks ahead to project; 0 gives the current tick boundary
	 * @param requestedTime the unix second to project from
	 * @param offsetMins   learned offset in minutes, or null when unknown
	 * @param offsetPrecisionMins granularity the offset was learned at, or null when unknown
	 */
	public static long tickTime(int tickRateMins, int ticks, long requestedTime,
		Integer offsetMins, Integer offsetPrecisionMins)
	{
		if (tickRateMins <= 0)
		{
			return 0L;
		}

		// All offsets are negative in reality but stored as positive.
		long offsetSeconds = 0L;
		if (isOffsetUsable(offsetPrecisionMins, offsetMins, tickRateMins))
		{
			offsetSeconds = (offsetMins % tickRateMins) * 60L;
		}

		long shifted = requestedTime + offsetSeconds;
		long currentTick = shifted - (shifted % (tickRateMins * 60L));
		long goalTick = currentTick + ((long) ticks * tickRateMins * 60L);
		return goalTick - offsetSeconds;
	}

	/**
	 * Estimate when a crop finishes growing.
	 *
	 * @param observedValueTime unix second at which the patch varbit was observed
	 * @param stage             growth stage at that observation
	 * @param stages            total stages for the crop's current state
	 * @param tickRateMins      minutes per growth tick, 0 if the crop does not grow further
	 * @return unix second of completion, or 0 when not estimable
	 */
	public static long completionTime(long observedValueTime, int stage, int stages,
		int tickRateMins, Integer offsetMins, Integer offsetPrecisionMins)
	{
		if (tickRateMins <= 0 || observedValueTime <= 0 || stage >= stages - 1)
		{
			return 0L;
		}

		long observedTick = tickTime(tickRateMins, 0, observedValueTime, offsetMins,
			offsetPrecisionMins);
		return tickTime(tickRateMins, stages - 1 - stage, observedTick, offsetMins,
			offsetPrecisionMins);
	}

	/**
	 * Project a stage forward to now, given when it was observed.
	 */
	public static int currentStage(long observedValueTime, long now, int stage, int stages,
		int tickRateMins, Integer offsetMins, Integer offsetPrecisionMins)
	{
		if (tickRateMins <= 0)
		{
			return stage;
		}

		long tickNow = tickTime(tickRateMins, 0, now, offsetMins, offsetPrecisionMins);
		long tickThen = tickTime(tickRateMins, 0, observedValueTime, offsetMins, offsetPrecisionMins);
		int delta = (int) ((tickNow - tickThen) / (tickRateMins * 60L));

		int projected = stage + delta;
		return projected >= stages ? stages - 1 : projected;
	}
}
