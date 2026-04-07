package videogoose.contracts.data.contract;

import api.network.PacketReadBuffer;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import videogoose.contracts.Contracts;
import videogoose.contracts.manager.ConfigManager;
import videogoose.contracts.utils.BlueprintUtils;
import videogoose.contracts.utils.FlavorUtils;
import videogoose.contracts.utils.SectorUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class BountyContract extends ContractData {

	public static final int PLAYER = 0;
	public static final int MOB = 1;

	private JSONObject targetData;

	protected BountyContract(int contractorID, String name, JSONObject targetData) {
		super(ContractType.BOUNTY, contractorID, name, targetData.getLong("reward"));
		this.targetData = targetData;
	}

	public BountyContract(int contractorID, String name, long reward, JSONObject targetData) {
		this(contractorID, name, targetData);
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
			return new BountyContract(factionId, contractName, targetData);
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
				int maxMobs = ConfigManager.getMaxBountyMobCount();
				double maxMass = ConfigManager.getMaxBountyMobCombinedMass();
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
		return json;
	}

	@Override
	public void deserialize(JSONObject data) {
		super.deserialize(data);
		targetData = data.getJSONObject("target");
	}

	@Override
	public ContractType getContractType() {
		return ContractType.BOUNTY;
	}

}
