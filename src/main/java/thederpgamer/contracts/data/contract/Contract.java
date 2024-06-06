package thederpgamer.contracts.data.contract;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.data.NetworkSerializable;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Contract implements JSONSerializable, NetworkSerializable {

	protected String name;
	protected int contractorID;
	protected long reward;
	protected HashMap<String, Long> claimants = new HashMap<>();
	protected String uid;

	protected Contract(int contractorID, String name, long reward) {
		this.name = name;
		this.contractorID = contractorID;
		this.reward = reward;
		uid = UUID.randomUUID().toString();
		uid = uid.substring(0, uid.indexOf('-'));
		claimants = new HashMap<>();
	}

	protected Contract(PacketReadBuffer readBuffer) throws IOException {
		readFromBuffer(readBuffer);
	}

	protected Contract(JSONObject jsonObject) {
		fromJSON(jsonObject);
	}

	@Override
	public void fromJSON(JSONObject json) {
		name = json.getString("name");
		contractorID = json.getInt("contractorID");
		reward = json.getLong("reward");
		uid = json.getString("uid");
		claimants = new HashMap<>();
		JSONArray claimantsArray = json.getJSONArray("claimants");
		JSONArray claimantsTimeArray = json.getJSONArray("claimantsTime");
		for(int i = 0; i < claimantsArray.length(); i++) {
			claimants.put(claimantsArray.getString(i), claimantsTimeArray.getLong(i));
		}
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("type", getContractType().displayName);
		json.put("name", name);
		json.put("contractorID", contractorID);
		json.put("reward", reward);
		json.put("uid", uid);
		JSONArray claimantsArray = new JSONArray();
		JSONArray claimantsTimeArray = new JSONArray();
		for(String playerData : claimants.keySet()) {
			claimantsArray.put(playerData);
			claimantsTimeArray.put(claimants.get(playerData));
		}
		json.put("claimants", claimantsArray);
		json.put("claimantsTime", claimantsTimeArray);
		return json;
	}

	public static Contract readContract(JSONObject json) throws IOException {
		ContractType type = ContractType.fromString(json.getString("type"));
		switch(type) {
			case BOUNTY:
				return new BountyContract(json);
			case ITEMS:
				return new ItemsContract(json);
			default:
				return null;
		}
	}

	public static Contract readContract(PacketReadBuffer packetReadBuffer) throws IOException {
		ContractType type = ContractType.fromString(packetReadBuffer.readString());
		switch(type) {
			case BOUNTY:
				return new BountyContract(packetReadBuffer);
			case ITEMS:
				return new ItemsContract(packetReadBuffer);
			default:
				return null;
		}
	}

	public HashMap<String, Long> getClaimants() {
		return claimants;
	}

	public String getName() {
		return name;
	}

	public Faction getContractor() {
		if(GameCommon.getGameState().getFactionManager().existsFaction(contractorID))
			return GameCommon.getGameState().getFactionManager().getFaction(contractorID);
		else {
			ServerDataManager.removeContract(this);
			return null;
		}
	}

	public String getContractorName() {
		return (contractorID != 0) ? getContractor().getName() : "Non-Aligned";
	}

	public long getReward() {
		return reward;
	}

	public String getUID() {
		return uid;
	}

	public abstract boolean canComplete(PlayerState player);

	public abstract ContractType getContractType();

	public abstract void onCompletion(PlayerState player);

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		name = readBuffer.readString();
		contractorID = readBuffer.readInt();
		reward = readBuffer.readLong();
		uid = readBuffer.readString();
		int claimantsSize = readBuffer.readInt();
		for(int i = 0; i < claimantsSize; i++) {
			String playerName = readBuffer.readString();
			long time = readBuffer.readLong();
			claimants.put(playerName, time);
		}
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(getContractType().displayName);
		writeBuffer.writeString(name);
		writeBuffer.writeInt(contractorID);
		writeBuffer.writeLong(reward);
		writeBuffer.writeString(uid);
		writeBuffer.writeInt(claimants.size());
		for(String playerData : claimants.keySet()) {
			writeBuffer.writeString(playerData);
			writeBuffer.writeLong(claimants.get(playerData));
		}
	}

	public boolean canClaim(PlayerState playerState) {
		return !claimants.containsKey(playerState.getName()) && playerState.getFactionId() != contractorID && !GameCommon.getGameState().getFactionManager().isEnemy(contractorID, playerState.getFactionId());
	}

	public long getTimeRemaining(String player) {
		if(claimants.containsKey(player)) return claimants.get(player);
		return 0;
	}

	public enum ContractType {
		ALL("All"),
		BOUNTY("Bounty"),
		ITEMS("Items");
		//ESCORT("Escort");

		public final String displayName;

		ContractType(String displayName) {
			this.displayName = displayName;
		}

		public static ContractType fromString(String s) {
			for(ContractType type : values()) {
				if(s.trim().equalsIgnoreCase(type.displayName.trim())) return type;
			}
			return null;
		}

		public static ContractType getRandomType() {
			return values()[(int) (Math.random() * (values().length - 1)) + 1];
		}
	}
}
