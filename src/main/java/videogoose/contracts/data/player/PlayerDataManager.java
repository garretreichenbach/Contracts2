package videogoose.contracts.data.player;

import api.common.GameClient;
import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.SerializableData;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerDataManager extends DataManager<PlayerData> {

	private final Set<PlayerData> clientCache = new HashSet<>();
	private static PlayerDataManager instance;
	
	public static PlayerDataManager getInstance(boolean server) {
		if(instance == null) {
			instance = new PlayerDataManager();
			if(!server) instance.requestFromServer();
		}
		return instance;
	}

	@Override
	public Set<PlayerData> getServerCache() {
		return PersistentObjectUtil.getObjects(Contracts.getInstance().getSkeleton(), PlayerData.class)
				.stream().map(o -> (PlayerData) o).collect(Collectors.toSet());
	}

	@Override
	public SerializableData.DataType getDataType() {
		return SerializableData.DataType.PLAYER_DATA;
	}

	@Override
	public Set<PlayerData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(PlayerData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(PlayerData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(PlayerData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	@Override
	public void createMissingData(Object... args) {
		try {
			PlayerState playerState = GameCommon.getPlayerFromName((String) args[0]);
			PlayerData data = getFromName((String) args[0], true);
			if(playerState != null && data == null) {
				PersistentObjectUtil.addObject(Contracts.getInstance().getSkeleton(), new PlayerData(playerState));
				PersistentObjectUtil.save(Contracts.getInstance().getSkeleton());
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while initializing player data", exception);
		}
	}

	public PlayerData getFromName(String name, boolean server) {
		return (server ? getServerCache() : getClientCache()).stream()
				.filter(data -> data.getName().equals(name))
				.findFirst().orElse(null);
	}

	public Set<PlayerData> getFactionMembers(Faction faction) {
		return getFactionMembers(faction.getIdFaction());
	}

	public Set<PlayerData> getFactionMembers(int factionId) {
		return getServerCache().stream()
				.filter(data -> data.getFactionID() == factionId)
				.collect(Collectors.toSet());
	}

	public PlayerData getClientOwnData() {
		return getFromName(GameClient.getClientPlayerState().getName(), false);
	}
	
	public boolean dataExistsForPlayer(String playerName, boolean server) {
		// Check if data exists for the specified player
		if(server) {
			return getFromName(playerName, true) != null;
		} else {
			// Client check
			return getFromName(playerName, false) != null;
		}
	}
}
