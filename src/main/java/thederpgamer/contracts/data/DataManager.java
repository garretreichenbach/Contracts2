package thederpgamer.contracts.data;

import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.ContractData;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.data.player.PlayerDataManager;
import thederpgamer.contracts.networking.SendDataPacket;
import thederpgamer.contracts.networking.SyncRequestPacket;

import java.util.Collection;
import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public abstract class DataManager<E extends SerializableData> {

	public static final int ADD_DATA = 0;
	public static final int REMOVE_DATA = 1;
	public static final int UPDATE_DATA = 2;

	public static DataManager<?> getDataManager(Class<? extends DataManager<?>> dataManagerClass, boolean server) {
		if(dataManagerClass.equals(PlayerData.class)) {
			return PlayerDataManager.getInstance(server);
		} else if(dataManagerClass.equals(ContractData.class)) {
			return ContractDataManager.getInstance(server);
		}
		return null;
	}

	public void sendDataToAllPlayers(SerializableData data, int type) {
		for(PlayerState player : GameServer.getServerState().getPlayerStatesByName().values()) {
			sendDataToPlayer(player, data, type);
		}
	}

	public void sendDataToPlayer(PlayerState player, SerializableData data, int type) {
		Contracts.getInstance().logInfo("[SERVER] Sending " + data.getDataType().name() + " " + data.getUUID() + " to player " + player.getName() + " with type " + getTypeString(type) + ".");
		PacketUtil.sendPacket(player, new SendDataPacket(data, type)); // Send the packet to the specific player
	}

	public void sendAllDataToPlayer(PlayerState player) {
		Collection<E> cache = getCache(true);
		for(E data : cache) {
			sendDataToPlayer(player, data, ADD_DATA);
		}
	}

	public void requestFromServer() {
		Contracts.getInstance().logInfo("[CLIENT] Requesting all data from server for " + getDataType() + ".");
		PacketUtil.sendPacketToServer(new SyncRequestPacket(getDataType()));
	}

	public void sendPacket(SerializableData data, int type, boolean toServer) {
		Contracts.getInstance().logInfo((toServer ? "[CLIENT]" : "[SERVER]") + " Sending " + data.getDataType().name() + " " + data.getUUID() + " with type " + getTypeString(type) + ".");
		if(toServer) {
			PacketUtil.sendPacketToServer(new SendDataPacket(data, type));
		} else {
			sendDataToAllPlayers(data, type);
		}
	}

	public Collection<E> getCache(boolean isServer) {
		return isServer ? getServerCache() : getClientCache();
	}

	public void addData(E data, boolean server) {
		Contracts.getInstance().logInfo("Adding " + data.getDataType().name() + " " + data.getUUID() + " to " + (server ? "server" : "client") + " cache.");
		if(server) {
			addToServerCache(data);
		} else {
			addToClientCache(data);
		}
	}

	public void removeData(E data, boolean server) {
		Contracts.getInstance().logInfo("Removing " + data.getDataType().name() + " " + data.getUUID() + " from " + (server ? "server" : "client") + " cache.");
		if(server) {
			removeFromServerCache(data);
		} else {
			removeFromClientCache(data);
		}
	}

	public void updateData(E data, boolean server) {
		Contracts.getInstance().logInfo("Updating " + data.getDataType().name() + " " + data.getUUID() + " in " + (server ? "server" : "client") + " cache.");
		if(server) {
			updateServerCache(data);
		} else {
			updateClientCache(data);
		}
	}

	public void handlePacket(SerializableData data, int type, boolean server) {
		Contracts.getInstance().logInfo(server ? "[SERVER]" : "[CLIENT]" + " Received " + data.getDataType().name() + " " + data.getUUID() + " with type " + getTypeString(type) + ".");
		switch(type) {
			case ADD_DATA:
				addData((E) data, server);
				break;
			case REMOVE_DATA:
				removeData((E) data, server);
				break;
			case UPDATE_DATA:
				updateData((E) data, server);
				break;
		}
	}

	public E getFromUUID(String uuid, boolean server) {
		Collection<E> cache = getCache(server);
		for(E data : cache) if(data.getUUID().equals(uuid)) return data;
		return null;
	}

	public abstract Collection<E> getServerCache();

	public void addToServerCache(E data) {
		PersistentObjectUtil.addObject(Contracts.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(Contracts.getInstance().getSkeleton());
		sendDataToAllPlayers(data, ADD_DATA);
	}

	public void removeFromServerCache(E data) {
		PersistentObjectUtil.removeObject(Contracts.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(Contracts.getInstance().getSkeleton());
		sendDataToAllPlayers(data, REMOVE_DATA);
	}

	public void updateServerCache(E data) {
		PersistentObjectUtil.removeObject(Contracts.getInstance().getSkeleton(), data);
		PersistentObjectUtil.addObject(Contracts.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(Contracts.getInstance().getSkeleton());
		sendDataToAllPlayers(data, UPDATE_DATA);
	}

	protected String getTypeString(int type) {
		switch(type) {
			case ADD_DATA:
				return "ADD_DATA";
			case REMOVE_DATA:
				return "REMOVE_DATA";
			case UPDATE_DATA:
				return "UPDATE_DATA";
			default:
				throw new IllegalArgumentException("Invalid type: " + type + ". Must be one of ADD_DATA, REMOVE_DATA, or UPDATE_DATA.");
		}
	}

	public abstract SerializableData.DataType getDataType();

	public abstract Set<E> getClientCache();

	public abstract void addToClientCache(E data);

	public abstract void removeFromClientCache(E data);

	public abstract void updateClientCache(E data);

	public abstract void createMissingData(Object... args);
}
