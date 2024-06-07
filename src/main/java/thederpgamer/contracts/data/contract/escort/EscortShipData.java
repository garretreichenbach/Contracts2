package thederpgamer.contracts.data.contract.escort;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.ItemStack;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.data.NetworkSerializable;
import thederpgamer.contracts.utils.BlueprintUtils;
import thederpgamer.contracts.utils.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class EscortShipData implements JSONSerializable, NetworkSerializable {

	private String bpName;
	private double mass;
	private final HashMap<Long, ItemStack[]> inventory = new HashMap<>();

	public EscortShipData(String bpName, double mass, HashMap<Long, ItemStack[]> inventory) {
		this.bpName = bpName;
		this.mass = mass;
		if(inventory != null) this.inventory.putAll(inventory);
	}

	public EscortShipData(JSONObject json) {
		fromJSON(json);
	}

	public EscortShipData(PacketReadBuffer readBuffer) throws IOException {
		readFromBuffer(readBuffer);
	}

	public static EscortShipData generateRandomEscort() {
		BlueprintEntry entry = BlueprintUtils.getRandomEscortShip();
		if(entry != null) return new EscortShipData(entry.getName(), entry.getMass(), null);
		return null;
	}

	public static EscortShipData generateRandomCargo(ArrayList<ItemStack> cargo) {
		//Transfer some of the cargo based on the ship's inventory size and how much cargo there is
		BlueprintEntry entry = BlueprintUtils.getRandomCargoShip();
		if(entry != null) {
			double totalCargoCap = entry.getTotalCapacity();
			if(totalCargoCap > 0) {
				Long2DoubleOpenHashMap cargoPoints = (Long2DoubleOpenHashMap) ClassUtils.getField(entry, "cargoPoints");
				if(cargoPoints != null) {
					HashMap<Long, ItemStack[]> inventory = new HashMap<>();
					int index = 0;
					while(index < cargo.size()) {
						for(Map.Entry<Long, Double> e : cargoPoints.long2DoubleEntrySet()) {
							long key = e.getKey();
							double availableVolume = e.getValue();
							double totalVolume = 0;
							for(int i = index; i < cargo.size(); i++) {
								ItemStack item = cargo.get(i);
								double volume = item.getElementInfo().getVolume() * item.getAmount();
								if(totalVolume + volume <= availableVolume) {
									totalVolume += volume;
									index++;
								} else break;
							}
							ItemStack[] items = new ItemStack[index];
							for(int i = 0; i < index; i++) items[i] = cargo.get(i);
							inventory.put(key, items);
						}
					}
					return new EscortShipData(entry.getName(), entry.getMass(), inventory);
				}
			}
		}
		return null;
	}

	@Override
	public void fromJSON(JSONObject json) {
		bpName = json.getString("bpName");
		mass = json.getDouble("mass");
		JSONObject inventoryJSON = json.getJSONObject("inventory");
		for(Object key : inventoryJSON.keySet()) {
			JSONArray itemArray = inventoryJSON.getJSONArray(key.toString());
			ItemStack[] items = new ItemStack[itemArray.length()];
			for(int i = 0; i < items.length; i++) {
				JSONObject itemJSON = itemArray.getJSONObject(i);
				short id = (short) itemJSON.getInt("id");
				int amount = itemJSON.getInt("amount");
				items[i] = new ItemStack(id, amount);
			}
			inventory.put(Long.parseLong(key.toString()), items);
		}
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("bpName", bpName);
		json.put("mass", mass);
		JSONObject inventoryJSON = new JSONObject();
		for(Long key : inventory.keySet()) {
			JSONArray itemArray = new JSONArray();
			for(ItemStack item : inventory.get(key)) {
				JSONObject itemJSON = new JSONObject();
				itemJSON.put("id", item.getId());
				itemJSON.put("amount", item.getAmount());
				itemArray.put(itemJSON);
			}
			inventoryJSON.put(key.toString(), itemArray);
		}
		json.put("inventory", inventoryJSON);
		return json;
	}

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		bpName = readBuffer.readString();
		mass = readBuffer.readDouble();
		int inventorySize = readBuffer.readInt();
		for(int i = 0; i < inventorySize; i++) {
			long key = readBuffer.readLong();
			int itemSize = readBuffer.readInt();
			ItemStack[] items = new ItemStack[itemSize];
			for(int j = 0; j < itemSize; j++) items[j] = new ItemStack(readBuffer.readShort(), readBuffer.readInt());
			inventory.put(key, items);
		}
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(bpName);
		writeBuffer.writeDouble(mass);
		writeBuffer.writeInt(inventory.size());
		for(Long key : inventory.keySet()) {
			writeBuffer.writeLong(key);
			ItemStack[] items = inventory.get(key);
			writeBuffer.writeInt(items.length);
			for(ItemStack item : items) {
				writeBuffer.writeShort(item.getId());
				writeBuffer.writeInt(item.getAmount());
			}
		}
	}

	public String getBpName() {
		return bpName;
	}

	public double getMass() {
		return mass;
	}

	public HashMap<Long, ItemStack[]> getInventory() {
		return inventory;
	}
}
