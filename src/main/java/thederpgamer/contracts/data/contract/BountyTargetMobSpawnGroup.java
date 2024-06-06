package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.JSONSerializable;
import thederpgamer.contracts.data.NetworkSerializable;
import thederpgamer.contracts.utils.BlueprintUtils;
import thederpgamer.contracts.utils.FlavorUtils;
import thederpgamer.contracts.utils.SectorUtils;

import java.io.IOException;
import java.util.*;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class BountyTargetMobSpawnGroup implements JSONSerializable, NetworkSerializable {

	private final ArrayList<BountyTargetMob> mobList = new ArrayList<>();
	private String name;
	private Vector3i sector;

	public BountyTargetMobSpawnGroup(String name, Vector3i sector, BountyTargetMob... mobs) {
		this.name = name;
		this.sector = sector;
		Collections.addAll(mobList, mobs);
	}

	public BountyTargetMobSpawnGroup(JSONObject json) {
		fromJSON(json);
	}

	public BountyTargetMobSpawnGroup(PacketReadBuffer readBuffer) throws IOException {
		readFromBuffer(readBuffer);
	}

	public static BountyTargetMobSpawnGroup generateRandomSpawnGroup() {
		try {
			HashMap<BlueprintEntry, Float> spawnWeights = BlueprintUtils.getPirateSpawnWeights();
			if(!spawnWeights.isEmpty()) {
				ArrayList<BlueprintEntry> blueprints = new ArrayList<>(spawnWeights.keySet());
				Random random = new Random();
				int maxMobs = ConfigManager.getMainConfig().getInt("max-bounty-mob-count");
				double maxMass = ConfigManager.getMainConfig().getDouble("max-bounty-mob-combined-mass");
				ArrayList<BountyTargetMob> mobList = new ArrayList<>();
				double totalMass = 0.0;
				int mobCount = random.nextInt(maxMobs) + 1;
				for(int i = 0; i < mobCount; i++) {
					BlueprintEntry blueprint = blueprints.get(random.nextInt(blueprints.size()));
					float weight = spawnWeights.get(blueprint);
					if(random.nextFloat() <= weight) {
						BountyTargetMob mob = new BountyTargetMob(blueprint, FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.PIRATE));
						if(totalMass + mob.getMass() <= maxMass) {
							mobList.add(mob);
							totalMass += mob.getMass();
						}
					}
				}
				if(!mobList.isEmpty()) {
					String name = FlavorUtils.generateGroupName(FlavorUtils.FlavorType.PIRATE);
					Vector3i sector = SectorUtils.getRandomSector(10);
					return new BountyTargetMobSpawnGroup(name, sector, mobList.toArray(new BountyTargetMob[0]));
				}
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while generating a random BountyTargetMobSpawnGroup", exception);
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public Vector3i getSector() {
		return sector;
	}

	public ArrayList<BountyTargetMob> getMobList() {
		return mobList;
	}

	@Override
	public void fromJSON(JSONObject json) {
		name = json.getString("name");
		sector = Vector3i.parseVector3i(json.getString("sector"));
		JSONArray mobArray = json.getJSONArray("mobs");
		for(int i = 0; i < mobArray.length(); i++) mobList.add(new BountyTargetMob(mobArray.getJSONObject(i)));
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("sector", sector.toString());
		JSONArray mobArray = new JSONArray();
		for(BountyTargetMob mob : mobList) mobArray.put(mob.toJSON());
		json.put("mobs", mobArray);
		return json;
	}

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		name = readBuffer.readString();
		sector = Vector3i.parseVector3i(readBuffer.readString());
		int mobCount = readBuffer.readInt();
		for(int i = 0; i < mobCount; i++) mobList.add(new BountyTargetMob(readBuffer));
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(name);
		writeBuffer.writeString(sector.toStringPure());
		writeBuffer.writeInt(mobList.size());
		for(BountyTargetMob mob : mobList) mob.writeToBuffer(writeBuffer);
	}

	public long calculateReward() {
		long reward = 0L;
		for(BountyTargetMob mob : mobList) reward += mob.getPrice();
		return reward;
	}

	public List<SegmentController> spawnGroup() {
		List<SegmentController> spawnedMobs = new ArrayList<>();
		for(BountyTargetMob mob : mobList) {
			SegmentController spawnedMob = BlueprintUtils.spawnAsMob(mob, sector, FactionManager.PIRATES_ID);
			if(spawnedMob != null) spawnedMobs.add(spawnedMob);
		}
		return spawnedMobs;
	}

	public boolean isGroupDead(List<SegmentController> spawnedMobs) {
		for(SegmentController mob : spawnedMobs) {
			if(!isShipDead(mob)) return false;
		}
		return true;
	}

	public boolean isShipDead(SegmentController segmentController) {
		return !segmentController.existsInState() || segmentController.isMarkedForPermanentDelete() || segmentController.isCoreOverheating();
	}
}
