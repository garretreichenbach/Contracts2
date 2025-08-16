package thederpgamer.contracts.data.contract;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.contracts.data.SerializableData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public abstract class ContractData extends SerializableData {

	private final byte VERSION = 0;
	private ContractType type;
	protected String name;
	protected int contractorID;
	protected long reward;
	protected HashMap<String, Long> claimants = new HashMap<>();

	protected ContractData(ContractType type, int contractorID, String name, long reward) {
		super(DataType.CONTRACT_DATA);
		this.type = type;
		this.contractorID = contractorID;
		this.name = name;
		this.reward = reward;
	}

	protected ContractData(JSONObject jsonObject) {
		deserialize(jsonObject);
	}

	protected ContractData(PacketReadBuffer packetReadBuffer) throws IOException {
		deserializeNetwork(packetReadBuffer);
	}

	public static ContractData readContract(JSONObject json) throws IOException {
		ContractType type = ContractType.fromString(json.getString("type"));
		switch(Objects.requireNonNull(type)) {
			case BOUNTY:
				return new BountyContract(json);
			case ITEMS:
				return new ItemsContract(json);
			default:
				return null;
		}
	}

	public static ContractData readContract(PacketReadBuffer packetReadBuffer) throws IOException {
		ContractType type = ContractType.fromString(packetReadBuffer.readString());
		switch(Objects.requireNonNull(type)) {
			case BOUNTY:
				return new BountyContract(packetReadBuffer);
			case ITEMS:
				return new ItemsContract(packetReadBuffer);
			default:
				return null;
		}
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("name", name);
		data.put("contractor_id", contractorID);
		data.put("reward", reward);
		data.put("uuid", dataUUID);
		JSONArray claimantsArray = new JSONArray();
		for(String playerName : claimants.keySet()) {
			JSONObject claimantData = new JSONObject();
			claimantData.put("name", playerName);
			claimantData.put("time", claimants.get(playerName));
			claimantsArray.put(claimantData);
		}
		data.put("claimants", claimantsArray);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		name = data.getString("name");
		contractorID = data.getInt("contractor_id");
		reward = data.getLong("reward");
		dataUUID = data.getString("uuid");
		claimants = new HashMap<>();
		JSONArray claimantsArray = data.getJSONArray("claimants");
		for(int i = 0; i < claimantsArray.length(); i++) {
			JSONObject claimantData = claimantsArray.getJSONObject(i);
			String playerName = claimantData.getString("name");
			long time = claimantData.getLong("time");
			claimants.put(playerName, time);
		}
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(name);
		writeBuffer.writeInt(contractorID);
		writeBuffer.writeLong(reward);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeInt(claimants.size());
		for(String playerName : claimants.keySet()) {
			writeBuffer.writeString(playerName);
			writeBuffer.writeLong(claimants.get(playerName));
		}
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		name = readBuffer.readString();
		contractorID = readBuffer.readInt();
		reward = readBuffer.readLong();
		dataUUID = readBuffer.readString();
		int claimantsSize = readBuffer.readInt();
		claimants = new HashMap<>();
		for(int i = 0; i < claimantsSize; i++) {
			String playerName = readBuffer.readString();
			long time = readBuffer.readLong();
			claimants.put(playerName, time);
		}
	}

	public HashMap<String, Long> getClaimants() {
		return claimants;
	}

	public String getName() {
		return name;
	}

	public Faction getContractor() {
		if(GameCommon.getGameState().getFactionManager().existsFaction(contractorID)) {
			return GameCommon.getGameState().getFactionManager().getFaction(contractorID);
		} else {
			ContractDataManager.getInstance(GameCommon.getGameState().isOnServer()).removeData(this, GameCommon.getGameState().isOnServer());
			return null;
		}
	}

	public String getContractorName() {
		return (contractorID != 0) ? getContractor().getName() : "Non-Aligned";
	}

	public long getReward() {
		return reward;
	}

	public abstract boolean canComplete(PlayerState player);

	public abstract ContractType getContractType();

	public abstract void onCompletion(PlayerState player);

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
				if(s.trim().equalsIgnoreCase(type.displayName.trim())) {
					return type;
				}
			}
			return null;
		}

		public static ContractType getRandomType() {
			return values()[(int) (Math.random() * (values().length - 1)) + 1];
		}
	}
}
