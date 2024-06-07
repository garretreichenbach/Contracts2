package thederpgamer.contracts.data.contract.escort;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.ItemStack;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3fTools;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.player.faction.FactionManager;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.data.NetworkSerializable;
import thederpgamer.contracts.networking.server.ServerDataManager;
import thederpgamer.contracts.utils.BlueprintUtils;
import thederpgamer.contracts.utils.FlavorUtils;
import thederpgamer.contracts.utils.SectorUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class EscortCargoData implements JSONSerializable, NetworkSerializable {

	private Vector3i startSector;
	private Vector3i endSector;
	private EscortShipData[] escortShips;

	public static EscortCargoData generateRandom() {
		Vector3i start = SectorUtils.getRandomSector(30);
		Vector3i end = SectorUtils.getRandomSector(start, 10);
		float distance = Vector3fTools.distance(start.toVector3f().x, start.toVector3f().y, start.toVector3f().z, end.toVector3f().x, end.toVector3f().y, end.toVector3f().z);
		//lower distance means lower difficulty and lower reward
		float difficulty = Math.min(1, Math.max(10, distance / 10));

		int totalItemStacks = (int) (difficulty * 5);
		ArrayList<ItemStack> cargo = new ArrayList<>();
		ArrayList<ElementInformation> possibleItems = ServerDataManager.getResourcesFilter();
		for(int i = 0; i < totalItemStacks; i++) {
			ElementInformation item = possibleItems.get((int) (Math.random() * possibleItems.size()));
			if(item.isShoppable() && item.isInRecipe() && !item.isDeprecated()) {
				int amount = (int) (Math.random() * 1000);
				cargo.add(new ItemStack(item.getId(), amount));
			}
		}

		//Now we need to choose a number of cargo ships to generate based off the difficulty and amount of items, and distribute the items between the inventories of each ship
		int totalShips = (int) difficulty;
		//Some ships will be escorts rather than cargo ships, higher difficulty means less escorts and more cargo ships to protect
		//You don't have to protect the escort ships, but get a bonus reward for each one that survives
		//You do have to protect the cargo ships, and each one that gets destroyed will reduce the reward you get based off the total value of the cargo it was carrying
		int escortShips = (int) (totalShips * (1 - (difficulty / 10))); //There are fewer escorts the higher the difficulty, meaning more cargo ships to protect
		EscortShipData[] escortShipData = new EscortShipData[escortShips];
		int cargoShips = totalShips - escortShips;
		assert cargoShips > 0 : "There must be at least one cargo ship";
		EscortShipData[] cargoShipData = new EscortShipData[totalShips];
		for(int i = 0; i < totalShips; i++) {
			if(i < escortShips) {
				EscortShipData data = EscortShipData.generateRandomEscort();
				if(data == null) i --;
				else escortShipData[i] = data;
			} else {
				EscortShipData data = EscortShipData.generateRandomCargo(cargo);
				if(data == null) i --;
				else cargoShipData[i - escortShips] = data;
			}
		}
		return new EscortCargoData(start, end, escortShipData);
	}

	public EscortCargoData(Vector3i startSector, Vector3i endSector, EscortShipData... escortShips) {
		this.startSector = startSector;
		this.endSector = endSector;
		this.escortShips = escortShips;
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
		escortShips = new EscortShipData[json.getJSONArray("escortShips").length()];
		for(int i = 0; i < escortShips.length; i++) {
			escortShips[i] = new EscortShipData(json.getJSONArray("escortShips").getJSONObject(i));
		}
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("startSector", startSector.toStringPure());
		json.put("endSector", endSector.toStringPure());
		for(int i = 0; i < escortShips.length; i++) {
			json.append("escortShips", escortShips[i].toJSON());
		}
		return json;
	}

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		startSector = Vector3i.parseVector3i(readBuffer.readString());
		endSector = Vector3i.parseVector3i(readBuffer.readString());
		escortShips = new EscortShipData[readBuffer.readInt()];
		for(int i = 0; i < escortShips.length; i++) {
			escortShips[i] = new EscortShipData(readBuffer);
		}
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(startSector.toStringPure());
		writeBuffer.writeString(endSector.toStringPure());
		writeBuffer.writeInt(escortShips.length);
		for(EscortShipData escortShip : escortShips) {
			escortShip.writeToBuffer(writeBuffer);
		}
	}

	public List<SegmentController> spawnCargoShips() {
		List<SegmentController> spawnedShips = new ArrayList<>();
		for(EscortShipData escortShip : escortShips) {
			if(escortShip != null) {
				String bpName = escortShip.getBpName();
				String spawnName = FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.TRADERS);
				SegmentController controller = BlueprintUtils.spawnAsMob(bpName, spawnName, startSector, FactionManager.TRAIDING_GUILD_ID);
				if(controller != null) spawnedShips.add(controller);
			}
		}
		return spawnedShips;
	}

	public Vector3i getStartSector() {
		return startSector;
	}

	public Vector3i getEndSector() {
		return endSector;
	}

	public EscortShipData[] getEscortShips() {
		return escortShips;
	}

	public EscortShipData getShipData(SegmentController ship) {
		for(EscortShipData escortShip : escortShips) {
			if(escortShip.getBpName().equals(ship.blueprintIdentifier)) return escortShip;
		}
		return null;
	}
}
