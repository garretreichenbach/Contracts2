package videogoose.contracts.data.player;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import videogoose.contracts.data.SerializableData;

import java.io.IOException;
import java.util.*;

public class PlayerData extends SerializableData {

	private final byte VERSION = 1;

	private String name;
	private ArrayList<String> contracts;
	private int factionID;
	private HashMap<Integer, List<Long>> aggressionKills = new HashMap<>();
	private HashMap<Integer, Integer> bountyCounts = new HashMap<>();

	public PlayerData(PlayerState playerState) {
		super(DataType.PLAYER_DATA);
		name = playerState.getName();
		contracts = new ArrayList<>();
		factionID = playerState.getFactionId();
		aggressionKills = new HashMap<>();
		bountyCounts = new HashMap<>();
	}

	public PlayerData(JSONObject json) {
		deserialize(json);
	}

	public PlayerData(PacketReadBuffer packetReadBuffer) throws IOException {
		deserializeNetwork(packetReadBuffer);
	}

	public void sendMail(String from, String title, String contents) {
		GameCommon.getPlayerFromName(name).getClientChannel().getPlayerMessageController().serverSend(from, name, title, contents);
	}

	public PlayerState getPlayerState() {
		return GameCommon.getPlayerFromName(name);
	}

	public boolean isOnServer() {
		return getPlayerState() != null && getPlayerState().isOnServer();
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("name", name);
		JSONArray contractArray = new JSONArray();
		for(String contract : contracts) {
			contractArray.put(contract);
		}
		data.put("contracts", contractArray);
		data.put("factionID", factionID);
		JSONObject aggressionJson = new JSONObject();
		for(Map.Entry<Integer, List<Long>> entry : aggressionKills.entrySet()) {
			JSONArray timestamps = new JSONArray();
			for(Long ts : entry.getValue()) timestamps.put(ts);
			aggressionJson.put(String.valueOf(entry.getKey()), timestamps);
		}
		data.put("aggression_kills", aggressionJson);
		JSONObject bountyJson = new JSONObject();
		for(Map.Entry<Integer, Integer> entry : bountyCounts.entrySet()) {
			bountyJson.put(String.valueOf(entry.getKey()), entry.getValue());
		}
		data.put("bounty_counts", bountyJson);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = data.has("version") ? (byte) data.getInt("version") : 0;
		name = data.getString("name");
		JSONArray contractArray = data.getJSONArray("contracts");
		contracts = new ArrayList<>();
		for(int i = 0; i < contractArray.length(); i++) {
			contracts.add(contractArray.getString(i));
		}
		factionID = data.getInt("factionID");
		aggressionKills = new HashMap<>();
		bountyCounts = new HashMap<>();
		if(version >= 1) {
			if(data.has("aggression_kills")) {
				JSONObject aggressionJson = data.getJSONObject("aggression_kills");
				for(Object keyObj : aggressionJson.keySet()) {
					String key = (String) keyObj;
					int factionId = Integer.parseInt(key);
					JSONArray timestamps = aggressionJson.getJSONArray(key);
					List<Long> kills = new ArrayList<>();
					for(int i = 0; i < timestamps.length(); i++) kills.add(timestamps.getLong(i));
					aggressionKills.put(factionId, kills);
				}
			}
			if(data.has("bounty_counts")) {
				JSONObject bountyJson = data.getJSONObject("bounty_counts");
				for(Object keyObj : bountyJson.keySet()) {
					String key = (String) keyObj;
					bountyCounts.put(Integer.parseInt(key), bountyJson.getInt(key));
				}
			}
		}
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(name);
		writeBuffer.writeStringList(contracts);
		writeBuffer.writeInt(factionID);
		writeBuffer.writeInt(aggressionKills.size());
		for(Map.Entry<Integer, List<Long>> entry : aggressionKills.entrySet()) {
			writeBuffer.writeInt(entry.getKey());
			writeBuffer.writeInt(entry.getValue().size());
			for(Long ts : entry.getValue()) writeBuffer.writeLong(ts);
		}
		writeBuffer.writeInt(bountyCounts.size());
		for(Map.Entry<Integer, Integer> entry : bountyCounts.entrySet()) {
			writeBuffer.writeInt(entry.getKey());
			writeBuffer.writeInt(entry.getValue());
		}
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		name = readBuffer.readString();
		contracts = readBuffer.readStringList();
		factionID = readBuffer.readInt();
		aggressionKills = new HashMap<>();
		bountyCounts = new HashMap<>();
		if(version >= 1) {
			int aggressionSize = readBuffer.readInt();
			for(int i = 0; i < aggressionSize; i++) {
				int factionId = readBuffer.readInt();
				int killCount = readBuffer.readInt();
				List<Long> kills = new ArrayList<>();
				for(int j = 0; j < killCount; j++) kills.add(readBuffer.readLong());
				aggressionKills.put(factionId, kills);
			}
			int bountySize = readBuffer.readInt();
			for(int i = 0; i < bountySize; i++) {
				int factionId = readBuffer.readInt();
				int count = readBuffer.readInt();
				bountyCounts.put(factionId, count);
			}
		}
	}

	public String getName() {
		return name;
	}

	public ArrayList<String> getContracts() {
		return contracts;
	}

	public int getFactionID() {
		return factionID;
	}

	public void setFactionID(int factionID) {
		this.factionID = factionID;
		PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
	}

	public HashMap<Integer, List<Long>> getAggressionKills() {
		return aggressionKills;
	}

	public HashMap<Integer, Integer> getBountyCounts() {
		return bountyCounts;
	}

	public void removeContract(String uuid) {
		if(contracts.remove(uuid)) {
			PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
		}
	}
}