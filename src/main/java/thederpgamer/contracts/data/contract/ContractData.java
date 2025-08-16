package thederpgamer.contracts.data.contract;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.contracts.data.SerializableData;

import java.io.IOException;
import java.util.Objects;

public abstract class ContractData extends SerializableData {

	private final byte VERSION = 0;
	private ContractType type;
	protected String name;
	protected int contractorID;
	protected long reward;

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
		data.put("uuid", dataUUID);
		data.put("type", type.name());
		data.put("name", name);
		data.put("contractor_id", contractorID);
		data.put("reward", reward);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		if(data.has("version")) {
			byte version = (byte) data.getInt("version");
			if(version != VERSION) {
				throw new IllegalStateException("Incompatible contract data version: " + version + " (expected: " + VERSION + ")");
			}
		}
		dataUUID = data.getString("uuid");
		type = ContractType.fromString(data.getString("type"));
		name = data.getString("name");
		contractorID = data.getInt("contractor_id");
		reward = data.getLong("reward");
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(type.name());
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(name);
		writeBuffer.writeInt(contractorID);
		writeBuffer.writeLong(reward);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		if(version != VERSION) {
			throw new IOException("Incompatible contract data version: " + version + " (expected: " + VERSION + ")");
		}
		dataUUID = readBuffer.readString();
		name = readBuffer.readString();
		contractorID = readBuffer.readInt();
		reward = readBuffer.readLong();
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

	public abstract ContractType getContractType();

	public enum ContractType {
		ALL("All"),
		BOUNTY("Bounty"),
		ITEMS("Items");

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
