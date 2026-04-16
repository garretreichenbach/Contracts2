package videogoose.contracts.manager;

import videogoose.contracts.data.player.PlayerData;
import videogoose.contracts.data.player.PlayerDataManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player kill counts against NPC factions, with time-based decay.
 * Each kill is recorded with a timestamp. Kills older than the configured
 * decay timer are pruned on access. Data is persisted through PlayerData.
 */
public class AggressionManager {

	private static AggressionManager instance;

	// playerName -> (factionId -> list of kill timestamps)
	private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, List<Long>>> aggressionMap = new ConcurrentHashMap<>();

	// playerName -> (factionId -> number of bounties placed so far)
	private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> bountyCountMap = new ConcurrentHashMap<>();

	// Tracks which players have been loaded from persistent storage
	private final Set<String> loadedPlayers = ConcurrentHashMap.newKeySet();

	public static AggressionManager getInstance() {
		if(instance == null) instance = new AggressionManager();
		return instance;
	}

	/**
	 * Loads aggression data from a PlayerData instance into the in-memory maps.
	 */
	public void loadFromPlayerData(PlayerData playerData) {
		String playerName = playerData.getName();
		if(!playerData.getAggressionKills().isEmpty()) {
			ConcurrentHashMap<Integer, List<Long>> killMap = new ConcurrentHashMap<>();
			for(Map.Entry<Integer, List<Long>> entry : playerData.getAggressionKills().entrySet()) {
				killMap.put(entry.getKey(), Collections.synchronizedList(new ArrayList<>(entry.getValue())));
			}
			aggressionMap.put(playerName, killMap);
		}
		if(!playerData.getBountyCounts().isEmpty()) {
			bountyCountMap.put(playerName, new ConcurrentHashMap<>(playerData.getBountyCounts()));
		}
	}

	/**
	 * Saves the in-memory aggression data back into the PlayerData instance.
	 */
	public void saveToPlayerData(PlayerData playerData) {
		String playerName = playerData.getName();
		ConcurrentHashMap<Integer, List<Long>> killMap = aggressionMap.get(playerName);
		if(killMap != null) {
			playerData.getAggressionKills().clear();
			for(Map.Entry<Integer, List<Long>> entry : killMap.entrySet()) {
				pruneExpired(entry.getValue());
				if(!entry.getValue().isEmpty()) {
					playerData.getAggressionKills().put(entry.getKey(), new ArrayList<>(entry.getValue()));
				}
			}
		} else {
			playerData.getAggressionKills().clear();
		}
		ConcurrentHashMap<Integer, Integer> bountyMap = bountyCountMap.get(playerName);
		if(bountyMap != null) {
			playerData.getBountyCounts().clear();
			playerData.getBountyCounts().putAll(bountyMap);
		} else {
			playerData.getBountyCounts().clear();
		}
	}

	/**
	 * Ensures a player's aggression data has been loaded from PlayerData.
	 */
	private void ensureLoaded(String playerName) {
		if(loadedPlayers.add(playerName)) {
			PlayerData playerData = PlayerDataManager.getInstance(true).getFromName(playerName, true);
			if(playerData != null) loadFromPlayerData(playerData);
		}
	}

	/**
	 * Records a kill by the given player against the given NPC faction.
	 * Persists the updated data to PlayerData.
	 *
	 * @return the new kill count after recording (post-decay pruning)
	 */
	public int recordKill(String playerName, int factionId) {
		ensureLoaded(playerName);
		aggressionMap.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>());
		ConcurrentHashMap<Integer, List<Long>> playerMap = aggressionMap.get(playerName);
		playerMap.computeIfAbsent(factionId, k -> Collections.synchronizedList(new ArrayList<>()));
		List<Long> kills = playerMap.get(factionId);
		kills.add(System.currentTimeMillis());
		pruneExpired(kills);
		persistPlayer(playerName);
		return kills.size();
	}

	/**
	 * Gets the current (post-decay) kill count for a player against a faction.
	 */
	public int getKillCount(String playerName, int factionId) {
		ensureLoaded(playerName);
		ConcurrentHashMap<Integer, List<Long>> playerMap = aggressionMap.get(playerName);
		if(playerMap == null) return 0;
		List<Long> kills = playerMap.get(factionId);
		if(kills == null) return 0;
		pruneExpired(kills);
		return kills.size();
	}

	/**
	 * Resets the kill count for a player against a specific faction.
	 * Called after a bounty is placed so the counter starts fresh.
	 */
	public void resetKills(String playerName, int factionId) {
		ConcurrentHashMap<Integer, List<Long>> playerMap = aggressionMap.get(playerName);
		if(playerMap != null) playerMap.remove(factionId);
		persistPlayer(playerName);
	}

	/**
	 * Increments the bounty count for a player against a faction and returns the new count.
	 * Persists the updated data to PlayerData.
	 */
	public int incrementBountyCount(String playerName, int factionId) {
		ensureLoaded(playerName);
		bountyCountMap.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>());
		ConcurrentHashMap<Integer, Integer> playerMap = bountyCountMap.get(playerName);
		int count = playerMap.merge(factionId, 1, Integer::sum);
		persistPlayer(playerName);
		return count;
	}

	/**
	 * Gets the number of bounties placed on a player by a specific faction.
	 */
	public int getBountyCount(String playerName, int factionId) {
		ensureLoaded(playerName);
		ConcurrentHashMap<Integer, Integer> playerMap = bountyCountMap.get(playerName);
		if(playerMap == null) return 0;
		return playerMap.getOrDefault(factionId, 0);
	}

	private void persistPlayer(String playerName) {
		PlayerData playerData = PlayerDataManager.getInstance(true).getFromName(playerName, true);
		if(playerData != null) {
			saveToPlayerData(playerData);
			PlayerDataManager.getInstance(true).updateData(playerData, true);
		}
	}

	private void pruneExpired(List<Long> kills) {
		long cutoff = System.currentTimeMillis() - ConfigManager.getAutoBountyDecayTimer();
		synchronized(kills) {
			kills.removeIf(aLong -> aLong < cutoff);
		}
	}
}
