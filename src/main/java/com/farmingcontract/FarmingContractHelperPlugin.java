package com.farmingcontract;

import com.farmingcontract.farming.CropState;
import com.farmingcontract.ui.FarmingContractHelperOverlay;
import com.farmingcontract.ui.FarmingContractHelperPanel;
import com.farmingcontract.ui.FarmingContractPatchOverlay;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

/**
 * Shows the player's current Farming Guild contract: the crop, its patch, the seed and protection
 * payment it needs, and how the patch is doing.
 *
 * <p>Read-only. This plugin never sends input, clicks, plants, pays or talks to anyone.
 */
@PluginDescriptor(
	name = "Farming Contract Helper",
	description = "Shows your current Farming Guild contract, its patch, seed and protection payment",
	tags = {"farming", "contract", "guild", "jane", "skilling", "patch", "seed"}
)
public class FarmingContractHelperPlugin extends Plugin
{
	@Inject
	private ClientThread clientThread;

	@Inject
	private Notifier notifier;

	@Inject
	private PatchTracker patchTracker;

	@Inject
	private FarmingContractHelperConfig config;

	@Inject
	private ContractManager contractManager;

	@Inject
	private InventoryChecker inventoryChecker;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FarmingContractHelperOverlay overlay;

	@Inject
	private FarmingContractPatchOverlay patchOverlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	private FarmingContractHelperPanel panel;

	private NavigationButton navButton;

	@Provides
	FarmingContractHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FarmingContractHelperConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new FarmingContractHelperPanel(itemManager, config);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/farmingcontract/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Farming Contract")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(overlay);
		overlayManager.add(patchOverlay);

		clientThread.invokeLater(() ->
		{
			contractManager.loadFromConfig();
			contractManager.observePatches();
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(patchOverlay);
		clientToolbar.removeNavigation(navButton);
		contractManager.persistCaches();
		patchTracker.clear();
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Covers logging in and hopping worlds; both re-read the persisted contract and patches.
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			contractManager.loadFromConfig();
			contractManager.observePatches();
			refresh();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			contractManager.persistCaches();
			patchTracker.clear();
		}
		else if (event.getGameState() == GameState.LOADING)
		{
			// Scene is being rebuilt; tracked objects are about to become stale.
			patchTracker.clear();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		contractManager.onProfileChanged();
		refresh();
	}

	@Subscribe
	public void onGameTick(net.runelite.api.events.GameTick event)
	{
		// Jane's dialogue only exists while the player is talking to her in the guild.
		if (contractManager.isInFarmingGuild() && contractManager.checkJaneDialogue())
		{
			contractManager.observePatches();
			refresh();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (contractManager.checkGameMessage(event.getMessage()))
		{
			refresh();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (contractManager.observePatches())
		{
			refresh();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (inventoryChecker.onContainerChanged(event.getContainerId(), event.getItemContainer()))
		{
			contractManager.persistCaches();
			refresh();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		patchTracker.onSpawned(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		patchTracker.onDespawned(event.getGameObject());
	}

	/**
	 * Rebuild the state snapshot and hand it to the UI. Also fires the ready notification once.
	 */
	private void refresh()
	{
		ContractState state = contractManager.buildState(config.checkBank(),
			config.checkSeedVault());

		if (panel != null)
		{
			panel.update(state);
		}
		overlay.setState(state);
		patchOverlay.setState(state);

		handleReadyNotification(state);
	}

	private void handleReadyNotification(ContractState state)
	{
		if (!state.hasContract())
		{
			return;
		}

		if (state.getCropState() == CropState.HARVESTABLE)
		{
			if (!contractManager.isNotifiedReady())
			{
				notifier.notify(config.readyNotification(),
					"Your " + state.getCrop().getContractName() + " contract is ready to check.");
				contractManager.markNotified();
			}
		}
		else if (state.isNotPlanted() || state.getCropState() == CropState.GROWING)
		{
			// Harvested, replanted or cleared: allow the next ready notification through.
			if (contractManager.isNotifiedReady())
			{
				contractManager.resetNotified();
			}
		}
	}
}
