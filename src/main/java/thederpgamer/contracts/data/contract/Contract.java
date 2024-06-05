package thederpgamer.contracts.data.contract;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Contract implements JSONSerializable {

	protected String name;
	protected int contractorID;
	protected int reward;
	protected HashMap<String, Long> claimants = new HashMap<>();
	protected String uid;

	protected Contract(int contractorID, String name, int reward) {
		this.name = name;
		this.contractorID = contractorID;
		this.reward = reward;
		uid = UUID.randomUUID().toString();
		uid = uid.substring(0, uid.indexOf('-'));
		claimants = new HashMap<>();
	}

	protected Contract(PacketReadBuffer readBuffer) throws IOException {
		name = readBuffer.readString();
		contractorID = readBuffer.readInt();
		reward = readBuffer.readInt();
		uid = readBuffer.readString();
		int claimantsSize = readBuffer.readInt();
		for(int i = 0; i < claimantsSize; i++) {
			String playerName = readBuffer.readString();
			long time = readBuffer.readLong();
			claimants.put(playerName, time);
		}
		readFromBuffer(readBuffer);
	}

	protected Contract(JSONObject jsonObject) {
		fromJSON(jsonObject);
	}

	@Override
	public void fromJSON(JSONObject json) {
		name = json.getString("name");
		contractorID = json.getInt("contractorID");
		reward = json.getInt("reward");
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

	public int getReward() {
		return reward;
	}

	public String getUID() {
		return uid;
	}

	public abstract boolean canComplete(PlayerState player);

	public abstract ContractType getContractType();

	public abstract void onCompletion(PlayerState player);

	public abstract void readFromBuffer(PacketReadBuffer readBuffer) throws IOException;

	public abstract void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException;

	public void writeContract(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeString(getContractType().displayName);
		packetWriteBuffer.writeString(name);
		packetWriteBuffer.writeInt(contractorID);
		packetWriteBuffer.writeInt(reward);
		packetWriteBuffer.writeString(uid);
		packetWriteBuffer.writeInt(claimants.size());
		for(String playerData : claimants.keySet()) {
			packetWriteBuffer.writeString(playerData);
			packetWriteBuffer.writeLong(claimants.get(playerData));
		}
		writeToBuffer(packetWriteBuffer);
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
