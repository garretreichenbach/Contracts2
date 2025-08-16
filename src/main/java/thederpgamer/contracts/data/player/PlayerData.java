package thederpgamer.contracts.data.player;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.SerializableData;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerData extends SerializableData {

	private final byte VERSION = 0;

	private String name;
	private ArrayList<String> contracts;
	private int factionID;

	public PlayerData(PlayerState playerState) {
		super(DataType.PLAYER_DATA);
		name = playerState.getName();
		contracts = new ArrayList<>();
		factionID = playerState.getFactionId();
	}

	public PlayerData(JSONObject json) {
		deserialize(json);
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
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		name = data.getString("name");
		JSONArray contractArray = data.getJSONArray("contracts");
		contracts = new ArrayList<>();
		for(int i = 0; i < contractArray.length(); i++) {
			contracts.add(contractArray.getString(i));
		}
		factionID = data.getInt("factionID");
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(name);
		writeBuffer.writeStringList(contracts);
		writeBuffer.writeInt(factionID);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		name = readBuffer.readString();
		contracts = readBuffer.readStringList();
		factionID = readBuffer.readInt();
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

	public void removeContract(String uuid) {
		if(contracts.remove(uuid)) {
			PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
		}
	}
}