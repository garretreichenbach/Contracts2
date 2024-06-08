package thederpgamer.contracts.data.contract.escort;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3fTools;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.data.element.ElementInformation;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.data.NetworkSerializable;
import thederpgamer.contracts.networking.server.ServerDataManager;
import thederpgamer.contracts.utils.SectorUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class EscortCargoData implements JSONSerializable, NetworkSerializable {

	private Vector3i startSector;
	private Vector3i endSector;
	private long reward;
	public ItemStack[] cargo;

	public static EscortCargoData generateRandom() {
		Vector3i start = SectorUtils.getRandomSector(50);
		Vector3i end = SectorUtils.getRandomSector(start, 50);
		float distance = Vector3fTools.distance(start.toVector3f().x, start.toVector3f().y, start.toVector3f().z, end.toVector3f().x, end.toVector3f().y, end.toVector3f().z);
		//lower distance means lower difficulty and lower reward
		float difficulty = Math.max(1, Math.min(10, distance / 10));

		long totalPrice = 0;
		int totalItemStacks = (int) (difficulty * 30);
		ArrayList<ItemStack> cargo = new ArrayList<>();
		ArrayList<ElementInformation> possibleItems = ServerDataManager.getResourcesFilter();
		for(int i = 0; i < totalItemStacks; i++) {
			ElementInformation item = possibleItems.get((int) (Math.random() * possibleItems.size()));
			if(item.isShoppable() && item.isInRecipe() && !item.isDeprecated()) {
				int amount = (int) (Math.random() * 1000);
				cargo.add(new ItemStack(item.getId(), amount));
				totalPrice += amount * item.getPrice(true) * amount;
			}
		}
		return new EscortCargoData(start, end, (long) (totalPrice * 1.3f), cargo.toArray(new ItemStack[0]));
	}

	public EscortCargoData(Vector3i startSector, Vector3i endSector, long reward, ItemStack[] cargo) {
		this.startSector = startSector;
		this.endSector = endSector;
		this.cargo = cargo;
		this.reward = reward;
	}

	public EscortCargoData(JSONObject json) {
		fromJSON(json);
	}

	public EscortCargoData(PacketReadBuffer readBuffer) throws IOException {
		readFromBuffer(readBuffer);
	}

	@Override
	public void fromJSON(JSONObject json) {
		startSector = Vector3i.parseVector3i(json.getString("startSector"));
		endSector = Vector3i.parseVector3i(json.getString("endSector"));
		reward = json.getLong("reward");
		JSONArray cargoArray = json.getJSONArray("cargo");
		cargo = new ItemStack[cargoArray.length()];
		for(int i = 0; i < cargo.length; i++) {
			JSONObject cargoObject = cargoArray.getJSONObject(i);
			cargo[i] = new ItemStack((short) cargoObject.getInt("id"), cargoObject.getInt("amount"));
		}
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("startSector", startSector.toStringPure());
		json.put("endSector", endSector.toStringPure());
		json.put("reward", reward);
		JSONArray cargoArray = new JSONArray();
		for(ItemStack stack : cargo) {
			JSONObject cargoObject = new JSONObject();
			cargoObject.put("id", stack.getId());
			cargoObject.put("amount", stack.getAmount());
			cargoArray.put(cargoObject);
		}
		json.put("cargo", cargoArray);
		return json;
	}

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		startSector = Vector3i.parseVector3i(readBuffer.readString());
		endSector = Vector3i.parseVector3i(readBuffer.readString());
		reward = readBuffer.readLong();
		cargo = new ItemStack[readBuffer.readInt()];
		for(int i = 0; i < cargo.length; i++) {
			cargo[i] = new ItemStack(readBuffer.readShort(), readBuffer.readInt());
		}
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(startSector.toStringPure());
		writeBuffer.writeString(endSector.toStringPure());
		writeBuffer.writeLong(reward);
		writeBuffer.writeInt(cargo.length);
		for(ItemStack stack : cargo) {
			writeBuffer.writeShort(stack.getId());
			writeBuffer.writeInt(stack.getAmount());
		}
	}

	public Vector3i getStartSector() {
		return startSector;
	}

	public Vector3i getEndSector() {
		return endSector;
	}

	public long getReward() {
		return reward;
	}

	public ElementCountMap toCountMap() {
		ElementCountMap countMap = new ElementCountMap();
		for(ItemStack stack : cargo) countMap.put(stack.getId(), stack.getAmount());
		return countMap;
	}
}
