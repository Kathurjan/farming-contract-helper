package com.farmingcontract.ui;

import com.farmingcontract.ContractState;
import com.farmingcontract.FarmingContractHelperConfig;
import com.farmingcontract.ItemAvailability;
import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.data.ProtectionPayment;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Compact on-screen summary of the contract, built from RuneLite's standard overlay components so
 * it matches every other plugin.
 */
@Singleton
public class FarmingContractHelperOverlay extends OverlayPanel
{
	private static final Color GOOD = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color BAD = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final Color UNSURE = ColorScheme.LIGHT_GRAY_COLOR;

	/**
	 * Have it, but in the bank or vault rather than carried.
	 */
	private static final Color STORAGE = ColorScheme.PROGRESS_INPROGRESS_COLOR;

	private final FarmingContractHelperConfig config;

	@Setter
	private ContractState state = ContractState.UNKNOWN;

	@Inject
	private FarmingContractHelperOverlay(FarmingContractHelperConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		getMenuEntries().add(new OverlayMenuEntry(
			net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG,
			"Configure", "Farming Contract Helper overlay"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Farming Contract")
			.color(ColorScheme.BRAND_ORANGE)
			.build());

		if (!state.hasContract())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Unknown")
				.right("Ask Jane")
				.rightColor(UNSURE)
				.build());
			panelComponent.setPreferredSize(new Dimension(160, 0));
			return super.render(graphics);
		}

		ContractCrop crop = state.getCrop();

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Contract:")
			.right(crop.getContractName())
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Patch:")
			.right(crop.getPatchType().getDisplayName())
			.build());

		panelComponent.getChildren().add(itemLine("Seed:",
			crop.getSeedQuantity() + " " + crop.getSeedName(), state.getSeed()));

		if (config.showProtection())
		{
			ProtectionPayment payment = crop.getProtection();
			if (payment.isAvailable())
			{
				panelComponent.getChildren().add(itemLine("Payment:",
					payment.getDescription(), state.getProtection()));
			}
			else
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Payment:")
					.right("Not available")
					.rightColor(UNSURE)
					.build());
			}
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right(state.getPatchStatusText())
			.rightColor(state.getCropState() == null ? UNSURE : state.getCropState().getColor())
			.build());

		String ready = readyText(state);
		if (ready != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Ready:")
				.right(ready)
				.build());
		}

		panelComponent.setPreferredSize(new Dimension(200, 0));
		return super.render(graphics);
	}

	/**
	 * A line marked with how available the item is. Uses the same four states as the side panel:
	 * carried, carried as a note, in storage, unknown, missing.
	 */
	private LineComponent itemLine(String left, String right, ItemAvailability availability)
	{
		if (!config.checkInventory() || availability.isNotRequired())
		{
			return LineComponent.builder().left(left).right(right).build();
		}

		String mark;
		Color color;
		switch (availability.getStatus())
		{
			case HAVE:
				mark = " ✓";
				color = GOOD;
				break;
			case HAVE_NOTED:
				mark = " ✓ (noted)";
				color = GOOD;
				break;
			case IN_STORAGE:
				mark = " ◆ " + availability.getStorageDescription();
				color = STORAGE;
				break;
			case MISSING:
				mark = " ✗";
				color = BAD;
				break;
			case UNKNOWN:
			default:
				mark = " ?";
				color = UNSURE;
				break;
		}

		return LineComponent.builder()
			.left(left)
			.right(right + mark)
			.rightColor(color)
			.build();
	}

	private static String readyText(ContractState state)
	{
		if (state.getCompletionTime() <= 0L)
		{
			return null;
		}

		Duration remaining = Duration.between(Instant.now(),
			Instant.ofEpochSecond(state.getCompletionTime()));
		if (remaining.isNegative() || remaining.isZero())
		{
			return "now";
		}

		long hours = remaining.toHours();
		long minutes = remaining.toMinutes() % 60;
		return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
	}
}
