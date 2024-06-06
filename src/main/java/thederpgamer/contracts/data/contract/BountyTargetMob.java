package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.data.NetworkSerializable;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class BountyTargetMob implements JSONSerializable, NetworkSerializable {

	private String bpName;
	private String spawnName;
	private double mass;
	private long price;

	public BountyTargetMob(String bpName, String spawnName, double mass, long price) {
		this.bpName = bpName;
		this.spawnName = spawnName;
		this.mass = mass;
		this.price = price;
	}

	public BountyTargetMob(BlueprintEntry entry, String spawnName) {
		this.spawnName = spawnName;
		bpName = entry.getName();
		mass = entry.getMass();
		price = entry.getPrice();
	}

	public BountyTargetMob(JSONObject json) {
		fromJSON(json);
	}

	public BountyTargetMob(PacketReadBuffer readBuffer) throws IOException {
		readFromBuffer(readBuffer);
	}

	public String getBPName() {
		return bpName;
	}

	public String getSpawnName() {
		return spawnName;
	}

	public double getMass() {
		return mass;
	}

	public long getPrice() {
		return price;
	}

	@Override
	public void fromJSON(JSONObject json) {
		bpName = json.getString("bpName");
		spawnName = json.getString("spawnName");
		mass = json.getDouble("mass");
		price = json.getLong("price");
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("bpName", bpName);
		json.put("spawnName", spawnName);
		json.put("mass", mass);
		json.put("price", price);
		return json;
	}

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		bpName = readBuffer.readString();
		spawnName = readBuffer.readString();
		mass = readBuffer.readDouble();
		price = readBuffer.readLong();
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(bpName);
		writeBuffer.writeString(spawnName);
		writeBuffer.writeDouble(mass);
		writeBuffer.writeLong(price);
	}
}
