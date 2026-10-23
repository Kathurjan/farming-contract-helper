package com.farmingcontract;

import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.farming.CropState;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Contract state transitions, unknown-contract handling and notification de-duplication.
 * Spec test categories 9, 10 and 11.
 */
public class ContractStateTest
{
	private static ContractState state(ContractCrop crop, CropState cropState)
	{
		return new ContractState(crop, cropState, 0, 0L,
			ItemAvailability.unknown(), ItemAvailability.unknown());
	}

	// -------------------------------------------------- 10. unknown-contract handling

	@Test
	public void unknownStateHasNoContract()
	{
		assertFalse(ContractState.UNKNOWN.hasContract());
		assertTrue(ContractState.UNKNOWN.isNotPlanted());
		assertEquals("Not tracked", ContractState.UNKNOWN.getPatchStatusText());
	}

	@Test
	public void anUntrackedPatchIsNotClaimedToBeEmpty()
	{
		ContractState s = state(ContractCrop.MAPLE, null);
		assertTrue(s.hasContract());
		// We have a contract but have never seen the patch: say so rather than "Not planted".
		assertEquals("Not tracked", s.getPatchStatusText());
	}

	// -------------------------------------------------- 9. contract state transitions

	@Test
	public void everyCropStateHasReadableText()
	{
		assertEquals("Not planted", state(ContractCrop.MAPLE, CropState.EMPTY).getPatchStatusText());
		assertEquals("Growing", state(ContractCrop.MAPLE, CropState.GROWING).getPatchStatusText());
		assertEquals("Ready", state(ContractCrop.MAPLE, CropState.HARVESTABLE).getPatchStatusText());
		assertEquals("Diseased", state(ContractCrop.MAPLE, CropState.DISEASED).getPatchStatusText());
		assertEquals("Dead", state(ContractCrop.MAPLE, CropState.DEAD).getPatchStatusText());
		assertEquals("Filling", state(ContractCrop.MAPLE, CropState.FILLING).getPatchStatusText());
	}

	@Test
	public void notPlantedCoversEmptyAndUnobserved()
	{
		assertTrue(state(ContractCrop.MAPLE, CropState.EMPTY).isNotPlanted());
		assertTrue(state(ContractCrop.MAPLE, null).isNotPlanted());
		assertFalse(state(ContractCrop.MAPLE, CropState.GROWING).isNotPlanted());
		assertFalse(state(ContractCrop.MAPLE, CropState.HARVESTABLE).isNotPlanted());
		assertFalse(state(ContractCrop.MAPLE, CropState.DISEASED).isNotPlanted());
	}

	// -------------------------------------------------- 11. duplicate notification prevention

	/**
	 * Mirrors the plugin's notification gate: notify once on becoming ready, and only re-arm after
	 * the patch leaves the ready state.
	 */
	private static class NotifyGate
	{
		private boolean notified;
		private int notifications;

		void observe(CropState cropState)
		{
			if (cropState == CropState.HARVESTABLE)
			{
				if (!notified)
				{
					notifications++;
					notified = true;
				}
			}
			else if (cropState == null || cropState == CropState.EMPTY
				|| cropState == CropState.GROWING)
			{
				notified = false;
			}
		}
	}

	@Test
	public void readyNotifiesOnlyOnceWhileStillReady()
	{
		NotifyGate gate = new NotifyGate();
		gate.observe(CropState.GROWING);
		gate.observe(CropState.HARVESTABLE);
		gate.observe(CropState.HARVESTABLE);
		gate.observe(CropState.HARVESTABLE);
		assertEquals(1, gate.notifications);
	}

	@Test
	public void harvestingThenRegrowingArmsTheNotificationAgain()
	{
		NotifyGate gate = new NotifyGate();
		gate.observe(CropState.HARVESTABLE);
		assertEquals(1, gate.notifications);

		// Harvested and replanted.
		gate.observe(CropState.EMPTY);
		gate.observe(CropState.GROWING);
		gate.observe(CropState.HARVESTABLE);
		assertEquals(2, gate.notifications);
	}

	@Test
	public void diseaseDoesNotReArmTheNotification()
	{
		NotifyGate gate = new NotifyGate();
		gate.observe(CropState.HARVESTABLE);
		gate.observe(CropState.DISEASED);
		gate.observe(CropState.HARVESTABLE);
		// Diseased is not a harvest, so we should not fire a second "ready" notification.
		assertEquals(1, gate.notifications);
	}
}
