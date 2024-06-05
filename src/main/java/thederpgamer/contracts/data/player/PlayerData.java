package thederpgamer.contracts.data.player;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.JSONSerializable;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerData implements JSONSerializable {

	public String name;
	public ArrayList<String> contracts;
	public int factionID;

	public PlayerData(JSONObject json) {
		fromJSON(json);
	}

	public PlayerData(PacketReadBuffer readBuffer) throws IOException {
		name = readBuffer.readString();
		contracts = new ArrayList<>();
		factionID = readBuffer.readInt();
	}

	public PlayerData(PlayerState playerState) {
		name = playerState.getName();
		contracts = new ArrayList<>();
		factionID = playerState.getFactionId();
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("contracts", contracts);
		json.put("factionID", factionID);
		return json;
	}

	@Override
	public void fromJSON(JSONObject json) {
		name = json.getString("name");
		contracts = new ArrayList<>();
		factionID = json.getInt("factionID");
	}

	public void sendMail(String from, String title, String contents) {
		GameCommon.getPlayerFromName(name).getClientChannel().getPlayerMessageController().serverSend(from, name, title, contents);
	}

	public void writeToBuffer(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeString(name);
		packetWriteBuffer.writeInt(factionID);
	}
}