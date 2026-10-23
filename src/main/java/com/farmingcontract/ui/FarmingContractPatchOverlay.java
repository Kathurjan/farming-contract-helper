package com.farmingcontract.ui;

import com.farmingcontract.ContractManager;
import com.farmingcontract.ContractState;
import com.farmingcontract.FarmingContractHelperConfig;
import com.farmingcontract.PatchTracker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines the Farming Guild patch the current contract belongs in, with a small label.
 *
 * <p>Patch objects come from {@link PatchTracker}, which maintains them from spawn and despawn
 * events. This overlay runs every frame, so it must not do any scene scanning itself.
 *
 * <p>Known limitation: the Farming Guild has two allotment patches sharing an object name, so an
 * allotment contract highlights both.
 */
@Singleton
public class FarmingContractPatchOverlay extends Overlay
{
	private static final Color OUTLINE = new Color(0, 200, 83, 180);
	private static final Color FILL = new Color(0, 200, 83, 30);

	private final FarmingContractHelperConfig config;
	private final ContractManager contractManager;
	private final PatchTracker patchTracker;

	@Setter
	private ContractState state = ContractState.UNKNOWN;

	@Inject
	private FarmingContractPatchOverlay(FarmingContractHelperConfig config,
		ContractManager contractManager, PatchTracker patchTracker)
	{
		this.config = config;
		this.contractManager = contractManager;
		this.patchTracker = patchTracker;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightPatch() || !state.hasContract()
			|| !contractManager.isInFarmingGuild())
		{
			return null;
		}

		String label = "Current contract: " + state.getCrop().getContractName();

		for (GameObject object : patchTracker.getPatches(state.getCrop().getPatchType()))
		{
			Shape hull = object.getConvexHull();
			if (hull == null)
			{
				continue;
			}

			OverlayUtil.renderPolygon(graphics, hull, OUTLINE, FILL, graphics.getStroke());
			OverlayUtil.renderTextLocation(graphics,
				object.getCanvasTextLocation(graphics, label, 60), label, OUTLINE);
		}

		return null;
	}
}
