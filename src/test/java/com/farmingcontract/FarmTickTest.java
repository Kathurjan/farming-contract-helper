package com.farmingcontract;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Farming tick arithmetic behind the completion-time estimate.
 */
public class FarmTickTest
{
	private static final int ALLOTMENT_TICK = 10;
	private static final int FRUIT_TREE_TICK = 160;

	@Test
	public void tickTimeAlignsToTheTickBoundary()
	{
		// With no learned offset, ticks align to the raw epoch boundary.
		long boundary = FarmTick.tickTime(ALLOTMENT_TICK, 0, 6_000L, null, null);
		assertEquals(0, boundary % (ALLOTMENT_TICK * 60L));
		assertTrue(boundary <= 6_000L);
	}

	@Test
	public void projectingTicksAheadAddsWholeTicks()
	{
		long now = FarmTick.tickTime(ALLOTMENT_TICK, 0, 6_000L, null, null);
		long threeAhead = FarmTick.tickTime(ALLOTMENT_TICK, 3, 6_000L, null, null);
		assertEquals(3L * ALLOTMENT_TICK * 60L, threeAhead - now);
	}

	@Test
	public void zeroTickRateIsNotEstimable()
	{
		assertEquals(0L, FarmTick.tickTime(0, 2, 6_000L, null, null));
		assertEquals(0L, FarmTick.completionTime(6_000L, 0, 5, 0, null, null));
	}

	@Test
	public void completionTimeCoversRemainingStages()
	{
		// Stage 1 of 5 means 3 more growth ticks to reach the final stage.
		long completion = FarmTick.completionTime(6_000L, 1, 5, ALLOTMENT_TICK, null, null);
		long observedTick = FarmTick.tickTime(ALLOTMENT_TICK, 0, 6_000L, null, null);
		assertEquals(3L * ALLOTMENT_TICK * 60L, completion - observedTick);
	}

	@Test
	public void fullyGrownCropHasNoCompletionEstimate()
	{
		assertEquals(0L, FarmTick.completionTime(6_000L, 4, 5, ALLOTMENT_TICK, null, null));
		assertEquals(0L, FarmTick.completionTime(6_000L, 9, 5, ALLOTMENT_TICK, null, null));
	}

	@Test
	public void unobservedPatchHasNoCompletionEstimate()
	{
		assertEquals(0L, FarmTick.completionTime(0L, 1, 5, ALLOTMENT_TICK, null, null));
	}

	@Test
	public void stageAdvancesWithElapsedTicks()
	{
		long observed = 600_000L;
		long twoTicksLater = observed + 2L * ALLOTMENT_TICK * 60L;
		int stage = FarmTick.currentStage(observed, twoTicksLater, 1, 5, ALLOTMENT_TICK, null, null);
		assertEquals(3, stage);
	}

	@Test
	public void stageIsClampedToTheFinalStage()
	{
		long observed = 600_000L;
		long muchLater = observed + 100L * ALLOTMENT_TICK * 60L;
		int stage = FarmTick.currentStage(observed, muchLater, 1, 5, ALLOTMENT_TICK, null, null);
		assertEquals(4, stage);
	}

	@Test
	public void offsetIsOnlyUsedWhenPreciseEnough()
	{
		// Learned at a coarser granularity than the crop's tick rate, and below the 40 minute
		// fallback threshold: not trustworthy.
		assertFalse(FarmTick.isOffsetUsable(10, 3, FRUIT_TREE_TICK));
		// Learned at 40 minutes or better is trusted for any crop.
		assertTrue(FarmTick.isOffsetUsable(40, 3, FRUIT_TREE_TICK));
		// Learned at least as precisely as this crop's tick rate.
		assertTrue(FarmTick.isOffsetUsable(10, 3, ALLOTMENT_TICK));
		assertFalse(FarmTick.isOffsetUsable(null, 3, ALLOTMENT_TICK));
		assertFalse(FarmTick.isOffsetUsable(10, null, ALLOTMENT_TICK));
	}

	@Test
	public void aUsableOffsetShiftsTheBoundary()
	{
		long without = FarmTick.tickTime(ALLOTMENT_TICK, 0, 6_000L, null, null);
		long with = FarmTick.tickTime(ALLOTMENT_TICK, 0, 6_000L, 3, 40);
		assertTrue(without != with);
		// The shift is the stored offset in minutes, modulo the tick rate.
		assertEquals(-3L * 60L, with - without);
	}
}
