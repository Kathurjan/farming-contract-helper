package com.farmingcontract;

import com.farmingcontract.data.PatchType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;

/**
 * Keeps track of the Farming Guild patch objects currently in the scene.
 *
 * <p>Driven by object spawn and despawn events rather than by scanning the scene, because the patch
 * highlight overlay runs every frame and walking the whole scene there would be far too expensive.
 * Farming patches are varbit-transformed objects, so changing crop or growth stage produces a
 * despawn followed by a spawn, which keeps this collection correct.
 */
@Singleton
public class PatchTracker
{
	private final Map<PatchType, List<GameObject>> patches = new EnumMap<>(PatchType.class);

	@Inject
	private Client client;

	public void onSpawned(GameObject object)
	{
		PatchType type = classify(object);
		if (type != null)
		{
			patches.computeIfAbsent(type, k -> new ArrayList<>()).add(object);
		}
	}

	public void onDespawned(GameObject object)
	{
		// The composition may already have transformed, so remove by identity from every bucket.
		for (List<GameObject> list : patches.values())
		{
			list.removeIf(tracked -> tracked == object
				|| tracked.getHash() == object.getHash());
		}
	}

	public void clear()
	{
		patches.clear();
	}

	public List<GameObject> getPatches(PatchType type)
	{
		List<GameObject> list = patches.get(type);
		return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
	}

	/**
	 * Which patch type this scene object is, or null if it is not a farming patch we care about.
	 * Resolves varbit-transformed objects to their impostor before reading the name.
	 */
	private PatchType classify(GameObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition == null)
		{
			return null;
		}

		if (composition.getImpostorIds() != null && composition.getImpostor() != null)
		{
			composition = composition.getImpostor();
		}

		String name = composition.getName();
		if (name == null || name.isEmpty() || "null".equals(name))
		{
			return null;
		}

		for (PatchType type : PatchType.values())
		{
			if (matchesName(name, type))
			{
				return type;
			}
		}
		return null;
	}

	/**
	 * Name matching for each patch type, kept static and side-effect free so it can be unit-tested.
	 *
	 * <p><b>Exact equality, deliberately.</b> Substring matching is wrong here: the Farming Guild
	 * contains a "Spirit tree patch" and a "Redwood tree patch" alongside the ordinary "Tree patch",
	 * so {@code contains("tree patch")} highlights three patches for one tree contract and also
	 * steals the redwood patch away from {@link PatchType#REDWOOD}.
	 */
	static boolean matchesName(String objectName, PatchType patchType)
	{
		return patchType.getObjectName().equalsIgnoreCase(objectName.trim());
	}
}
