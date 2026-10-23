package com.farmingcontract.ui;

import com.farmingcontract.ContractState;
import com.farmingcontract.FarmingContractHelperConfig;
import com.farmingcontract.ItemAvailability;
import com.farmingcontract.data.ContractCrop;
import com.farmingcontract.data.ContractTier;
import com.farmingcontract.data.ProtectionPayment;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.StringJoiner;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Side panel showing the current contract, broken into the sections the player needs while
 * prepping: what the contract is, where the patch is, what to plant, what to pay, and how it is
 * doing.
 */
public class FarmingContractHelperPanel extends PluginPanel
{
	private static final Color GOOD = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color BAD = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final Color UNSURE = ColorScheme.LIGHT_GRAY_COLOR;

	/**
	 * Have it, but it is in the bank or vault rather than on you.
	 */
	private static final Color STORAGE = ColorScheme.PROGRESS_INPROGRESS_COLOR;

	private final ItemManager itemManager;
	private final FarmingContractHelperConfig config;

	private final JLabel cropIcon = new JLabel();
	private final JLabel cropName = new JLabel();
	private final JPanel sections = new JPanel();

	public FarmingContractHelperPanel(ItemManager itemManager, FarmingContractHelperConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel(new BorderLayout(8, 0));
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		cropName.setFont(FontManager.getRunescapeBoldFont());
		cropName.setForeground(Color.WHITE);
		header.add(cropIcon, BorderLayout.WEST);
		header.add(cropName, BorderLayout.CENTER);

		sections.setLayout(new GridLayout(0, 1, 0, 8));

		add(header, BorderLayout.NORTH);
		add(sections, BorderLayout.CENTER);

		update(ContractState.UNKNOWN);
	}

	public void update(ContractState state)
	{
		SwingUtilities.invokeLater(() -> rebuild(state));
	}

	private void rebuild(ContractState state)
	{
		sections.removeAll();
		cropIcon.setIcon(null);

		if (!state.hasContract())
		{
			cropName.setText("Contract unknown");
			sections.add(section("", wrapped(
				"Talk to Guildmaster Jane and ask about your current contract.")));
			sections.revalidate();
			sections.repaint();
			return;
		}

		ContractCrop crop = state.getCrop();

		cropName.setText(crop.getContractName());
		AsyncBufferedImage image = itemManager.getImage(crop.getProduceItemId());
		cropIcon.setIcon(new ImageIcon(image));
		image.onLoaded(() -> cropIcon.setIcon(new ImageIcon(image)));

		sections.add(section("Contract", label(crop.getContractName() + tierSuffix(crop), Color.WHITE)));

		sections.add(section("Patch", multi(
			label(crop.getPatchType().getDisplayName() + " patch", Color.WHITE),
			wrapped(crop.getPatchType().getLocationDescription()))));

		sections.add(section("Required items", requiredItems(crop, state.getSeed())));

		if (config.showProtection())
		{
			sections.add(section("Protection", protectionSection(crop, state.getProtection())));
		}

		sections.add(section("Patch status", label(state.getPatchStatusText(),
			state.getCropState() == null ? UNSURE : state.getCropState().getColor())));

		sections.add(section("Completion time", label(completionText(state), Color.WHITE)));

		sections.revalidate();
		sections.repaint();
	}

	private static String tierSuffix(ContractCrop crop)
	{
		StringJoiner joiner = new StringJoiner("/");
		for (ContractTier tier : ContractTier.values())
		{
			if (crop.getTiers().contains(tier))
			{
				joiner.add(tier.getDisplayName());
			}
		}
		return "  (" + joiner + ")";
	}

	private JPanel requiredItems(ContractCrop crop, ItemAvailability seed)
	{
		String text = crop.getSeedQuantity() + " " + crop.getSeedName();
		if (!config.checkInventory())
		{
			return multi(label(text, Color.WHITE));
		}
		return multi(label(text, Color.WHITE), availabilityLabel(seed));
	}

	private JPanel protectionSection(ContractCrop crop, ItemAvailability protection)
	{
		ProtectionPayment payment = crop.getProtection();

		if (!payment.isAvailable())
		{
			return multi(label("Not available", UNSURE));
		}

		JPanel panel = multi(label(payment.getDescription(), Color.WHITE));

		if (payment.isContainerRequired())
		{
			panel.add(wrapped("Contents: " + payment.getContentsDescription()));
			panel.add(wrapped("The gardener only accepts filled "
				+ (payment.getQuantity() == 1 ? "containers" : "containers")
				+ " — loose produce must be put into them first."));
		}

		if (config.checkInventory())
		{
			panel.add(availabilityLabel(protection));
		}

		return panel;
	}

	/**
	 * Render availability honestly. Green when usable now, amber when the player has it but not on
	 * them, red only when we are certain they are short, grey when a container we would need has
	 * never been opened.
	 */
	private JLabel availabilityLabel(ItemAvailability availability)
	{
		switch (availability.getStatus())
		{
			case NOT_REQUIRED:
				return label("", UNSURE);

			case HAVE:
				return label("✓ In inventory (" + availability.getInventoryProgress() + ")", GOOD);

			case HAVE_NOTED:
				return label("✓ In inventory, noted ("
					+ availability.getInventoryProgress() + ")", GOOD);

			case IN_STORAGE:
			{
				String where = availability.getStorageDescription();
				String note = availability.hasUnusableNote() ? "  —  un-note to plant" : "";
				return label("◆ Not carried, in " + where + note, STORAGE);
			}

			case UNKNOWN:
			{
				StringBuilder sb = new StringBuilder();
				sb.append("? Inventory: ").append(availability.getInventoryProgress());
				String where = availability.getStorageDescription();
				if (where != null)
				{
					sb.append("  |  ").append(where);
				}
				sb.append("  |  elsewhere unknown");
				return label(sb.toString(), UNSURE);
			}

			case MISSING:
			default:
			{
				String note = availability.hasUnusableNote() ? "  (noted only — un-note first)" : "";
				return label("✗ Missing: " + availability.getInventoryProgress() + note, BAD);
			}
		}
	}

	private static String completionText(ContractState state)
	{
		if (state.getCompletionTime() <= 0L)
		{
			return state.isNotPlanted() ? "Not planted" : "Unknown";
		}

		Duration remaining = Duration.between(Instant.now(),
			Instant.ofEpochSecond(state.getCompletionTime()));
		if (remaining.isNegative() || remaining.isZero())
		{
			return "Ready";
		}

		long hours = remaining.toHours();
		long minutes = remaining.toMinutes() % 60;
		return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
	}

	// ------------------------------------------------------------------ small layout helpers

	private static JPanel section(String title, JPanel content)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		if (!title.isEmpty())
		{
			JLabel heading = new JLabel(title);
			heading.setFont(FontManager.getRunescapeSmallFont());
			heading.setForeground(ColorScheme.BRAND_ORANGE);
			panel.add(heading, BorderLayout.NORTH);
		}

		panel.add(content, BorderLayout.CENTER);
		return panel;
	}

	private static JPanel section(String title, JLabel content)
	{
		return section(title, multi(content));
	}

	private static JPanel multi(JLabel... labels)
	{
		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 2));
		panel.setOpaque(false);
		for (JLabel label : labels)
		{
			panel.add(label);
		}
		return panel;
	}

	private static JLabel label(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setForeground(color);
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	/**
	 * A label that wraps, for location descriptions and notes.
	 */
	private static JLabel wrapped(String text)
	{
		JLabel label = new JLabel("<html><body style='width:150px'>" + text + "</body></html>");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, label.getPreferredSize().height));
		return label;
	}
}
