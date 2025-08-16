package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.DataManager;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.utils.BlueprintUtils;
import thederpgamer.contracts.utils.FlavorUtils;
import thederpgamer.contracts.utils.SectorUtils;

import java.io.IOException;
import java.util.*;

public class BountyContract extends ContractData implements ActiveContractRunnable {

	public static final int PLAYER = 0;
	public static final int MOB = 1;

	private boolean killedTarget;
	private Vector3i sector;
	private JSONObject targetData;

	protected BountyContract(int contractorID, String name, Vector3i sector, JSONObject targetData) {
		super(ContractType.BOUNTY, contractorID, name, targetData.getLong("reward"));
		this.sector = sector;
		this.targetData = targetData;
	}

	public BountyContract(int contractorID, String name, long reward, JSONObject targetData) {
		this(contractorID, name, SectorUtils.getRandomSector(10), targetData);
		if(targetData.has("sector")) {
			JSONObject sectorData = targetData.getJSONObject("sector");
			sector = new Vector3i(sectorData.getInt("x"), sectorData.getInt("y"), sectorData.getInt("z"));
		} else {
			sector = new Vector3i();
		}
		killedTarget = false;
		this.reward = reward;
		this.name = name;
		this.contractorID = contractorID;
	}

	public BountyContract(PacketReadBuffer packetReadBuffer) throws IOException {
		super(packetReadBuffer);
	}

	public BountyContract(JSONObject json) {
		super(json);
	}

	public static BountyContract generateRandomMob(int factionId) {
		JSONObject targetData = generateMobTarget();
		if(targetData != null) {
			String name = FlavorUtils.generateGroupName(FlavorUtils.FlavorType.PIRATE);
			Vector3i sector = SectorUtils.getRandomSector(10);
			String contractName = "Defeat " + name + " in Sector " + sector;
			return new BountyContract(factionId, contractName, sector, targetData);
		}
		throw new IllegalStateException("Failed to generate a valid BountyContract for mobs.");
	}

	private static JSONObject generateMobTarget() {
		JSONObject targetData = new JSONObject();
		try {
			HashMap<BlueprintEntry, Float> spawnWeights = BlueprintUtils.getPirateSpawnWeights();
			if(!spawnWeights.isEmpty()) {
				ArrayList<BlueprintEntry> blueprints = new ArrayList<>(spawnWeights.keySet());
				Random random = new Random();
				int maxMobs = ConfigManager.getMainConfig().getInt("max-bounty-mob-count");
				double maxMass = ConfigManager.getMainConfig().getDouble("max-bounty-mob-combined-mass");
				ArrayList<JSONObject> mobList = new ArrayList<>();
				double totalMass = 0.0;
				long reward = 0L;
				int mobCount = random.nextInt(maxMobs) + 1;
				for(int i = 0; i < mobCount; i++) {
					BlueprintEntry blueprint = blueprints.get(random.nextInt(blueprints.size()));
					float weight = spawnWeights.get(blueprint);
					if(random.nextFloat() <= weight) {
						JSONObject mobData = new JSONObject();
						mobData.put("bp_name", blueprint.getName());
						mobData.put("spawn_name", FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.PIRATE));
						reward += blueprint.getPrice();
						double mass = blueprint.getMass();
						if(totalMass + mass <= maxMass) {
							mobList.add(mobData);
							totalMass += mass;
						}
					}
				}
				if(!mobList.isEmpty()) {
					targetData.put("mob_list", mobList);
					JSONObject sectorData = new JSONObject();
					Vector3i sector = SectorUtils.getRandomSector(10);
					sectorData.put("x", sector.x);
					sectorData.put("y", sector.y);
					sectorData.put("z", sector.z);
					targetData.put("sector", sectorData);
					targetData.put("target_type", MOB);
					targetData.put("reward", reward * 1.3f);
				} else {
					Contracts.getInstance().logWarning("No valid mobs generated for bounty target.");
					return null;
				}
			}
		} catch(Exception exception) {
			// Handle any exceptions that may occur during target generation
			Contracts.getInstance().logException("An error occurred while generating a random BountyTargetMob", exception);
			return null;
		}

		return targetData;
	}

	@Override
	public JSONObject serialize() {
		JSONObject json = super.serialize();
		json.put("target", targetData);
		json.put("killedTarget", killedTarget);
		return json;
	}

	@Override
	public void deserialize(JSONObject data) {
		super.deserialize(data);
		targetData = data.getJSONObject("target");
		killedTarget = data.getBoolean("killedTarget");
	}

	@Override
	public boolean canComplete(PlayerState player) {
		return killedTarget || (player.isOnServer() && player.isAdmin());
	}

	@Override
	public ContractType getContractType() {
		return ContractType.BOUNTY;
	}

	@Override
	public void onCompletion(PlayerState player) {

	}

	@Override
	public boolean canStartRunner(PlayerState player) {
        return player.getCurrentSector().equals(sector);
	}

	@Override
	public List<?> startRunner(PlayerState player) {
		return Collections.emptyList();
	}

	@Override
	public boolean updateRunner(PlayerState player, List<?> data) {
		return false;
	}

	public int getBountyType() {
		return targetData.getInt("target_type");
	}

	public Vector3i getSector() {
		JSONObject sectorData = targetData.getJSONObject("sector");
		return new Vector3i(sectorData.getInt("x"), sectorData.getInt("y"), sectorData.getInt("z"));
	}

	public JSONObject getTargetData() {
		return targetData;
	}

	public void setKilledTarget(boolean onServer, boolean killedTarget) {
		this.killedTarget = killedTarget;
		ContractDataManager.getInstance(onServer).sendPacket(this, DataManager.UPDATE_DATA, !onServer);
	}

   /* @Override
    public List<SegmentController> startRunner(PlayerState player) {
        return targetGroup.spawnGroup();
    }

    @Override
    public boolean updateRunner(PlayerState player, List<?> spawnedMobs) {
        if(targetGroup.isGroupDead((List<SegmentController>) spawnedMobs)) {
            killedTarget = true;
            return false;
        }
        return true;
    }

    @Override
    public void onCompletion(PlayerState player) {
        assert player.isOnServer();
        player.setCredits(player.getCredits() + reward);
    }

    @Override
    public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
        super.readFromBuffer(readBuffer);
        target = readBuffer.readString();
        killedTarget = readBuffer.readBoolean();
        targetType = BountyTargetType.values()[readBuffer.readInt()];
        if(targetType == BountyTargetType.NPC) {
            targetGroup = new BountyTargetMobSpawnGroup(readBuffer);
        }
    }

    @Override
    public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
        super.writeToBuffer(writeBuffer);
        writeBuffer.writeString(target);
        writeBuffer.writeBoolean(killedTarget);
        writeBuffer.writeInt(targetType.ordinal());
        if(targetType == BountyTargetType.NPC) targetGroup.writeToBuffer(writeBuffer);
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        target = json.getString("target");
        killedTarget = json.getBoolean("killedTarget");
        if(json.has("targetType")) targetType = BountyTargetType.valueOf(json.getString("targetType"));
        if(json.has("targetGroup")) targetGroup = new BountyTargetMobSpawnGroup(json.getJSONObject("targetGroup"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("target", target);
        json.put("killedTarget", killedTarget);
        json.put("targetType", targetType.toString());
        if(targetType == BountyTargetType.NPC) json.put("targetGroup", targetGroup.toJSON());
        return json;
    }

    public String getTarget() {
        return target;
    }

    public boolean hasKilledTarget() {
        return killedTarget;
    }

    public void setKilledTarget(boolean killedTarget) {
        this.killedTarget = killedTarget;
    }

    public BountyTargetType getTargetType() {
        return targetType;
    }

    public BountyTargetMobSpawnGroup getTargetGroup() {
        return targetGroup;
    }*/
}
