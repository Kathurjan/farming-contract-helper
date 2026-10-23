package com.farmingcontract.farming;

import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.data.PatchType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Sanity checks on the vendored varbit decode tables.
 *
 * <p>These do not re-verify RuneLite's data — that is copied verbatim under BSD-2 and is the
 * upstream project's responsibility. They confirm the vendoring did not lose or mangle anything:
 * every Farming Guild patch type still decodes, empty patches read as weeds, and the patch types
 * this plugin never queries are the inert stubs.
 */
public class PatchDecodeTest
{
	@Test
	public void everyContractPatchTypeDecodesAFreshPatch()
	{
		for (PatchType type : PatchType.values())
		{
			PatchState state = type.getImplementation().forVarbitValue(0);
			assertNotNull(type + " failed to decode varbit value 0", state);
			assertEquals(type + " should read as weeds when empty",
				Produce.WEEDS, state.getProduce());
		}
	}

	@Test
	public void decodersCoverTheWholeVarbitRangeWithoutThrowing()
	{
		for (PatchType type : PatchType.values())
		{
			for (int value = 0; value <= 255; value++)
			{
				// Must not throw; null is a legitimate "unrecognised value".
				type.getImplementation().forVarbitValue(value);
			}
		}
	}

	@Test
	public void nonGuildPatchTypesAreInertStubs()
	{
		// These exist only so the vendored Produce enum still compiles; they are never queried.
		assertNull(PatchImplementation.HOPS.forVarbitValue(0));
		assertNull(PatchImplementation.CALQUAT.forVarbitValue(4));
		assertNull(PatchImplementation.SPIRIT_TREE.forVarbitValue(1));
		assertNull(PatchImplementation.COMPOST.forVarbitValue(1));
		assertNull(PatchImplementation.MUSHROOM.forVarbitValue(5));
	}

	@Test
	public void everyContractCropIsReachableFromItsPatchDecoder()
	{
		// For each contract crop, some varbit value in its patch type must decode to that produce.
		for (ContractCrop crop : ContractCrop.values())
		{
			boolean found = false;
			for (int value = 0; value <= 255 && !found; value++)
			{
				PatchState state = crop.getPatchType().getImplementation().forVarbitValue(value);
				if (state != null && state.getProduce() == crop.getProduce())
				{
					found = true;
				}
			}
			assertTrue(crop.name() + " is not reachable from " + crop.getPatchType()
				+ "'s decode table", found);
		}
	}

	@Test
	public void growingAndHarvestableStagesAreConsistent()
	{
		// A tree patch mid-growth should report a positive tick rate and sane stage bounds.
		for (int value = 0; value <= 255; value++)
		{
			PatchState state = PatchType.TREE.getImplementation().forVarbitValue(value);
			if (state == null || state.getCropState() != CropState.GROWING)
			{
				continue;
			}
			assertTrue("stage should be within stages for value " + value,
				state.getStage() < state.getStages());
			assertTrue("growing crops should have a tick rate for value " + value,
				state.getTickRate() > 0);
		}
	}
}
