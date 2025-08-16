package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.utils.BlueprintUtils;
import thederpgamer.contracts.utils.FlavorUtils;
import thederpgamer.contracts.utils.SectorUtils;

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

	/*
	JSONArray spawns = stateData.optJSONArray("spawns");
		ArrayList<String> destroyed = new ArrayList<>();
		ArrayList<String> active = new ArrayList<>();
		boolean changed = false;
		if(spawns != null) {
			for(int i = 0; i < spawns.length(); i++) {
				JSONObject spawnData = spawns.getJSONObject(i);
				String status = spawnData.optString("status", "active");
				if(status.equals("active")) {
					int controllerId = spawnData.getInt("controller_id");
					Sendable sendable = GameCommon.getGameObject(controllerId);
					if(sendable == null) {
						// If the controller is not found, consider it destroyed.
						if(getBountyType() == MOB) {
							spawnData.put("status", "destroyed");
							setKilledTarget(playerState.isOnServer(), true);
							destroyed.add(spawnData.getString("spawn_name"));
							continue;
						}
					}
					if(!(sendable instanceof ManagedUsableSegmentController<?>)) {
						Contracts.getInstance().logWarning("Controller with ID " + controllerId + " is not a valid ManagedUsableSegmentController.");
						continue;
					}
					ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendable;
					if(controller.isMarkedForPermanentDelete() || controller.isMarkedForDeleteVolatile() || controller.isCoreOverheating()) {
						// If the controller is marked for deletion or overheating, consider the target killed.
						if(getBountyType() == MOB && !destroyed.contains(spawnData.getString("spawn_name"))) {
							spawnData.put("status", "destroyed");
							setKilledTarget(playerState.isOnServer(), true);
							destroyed.add(spawnData.getString("spawn_name"));
							changed = true;
						}
					} else {
						// If the controller is still active, add it to the active list.
						active.add(spawnData.getString("spawn_name"));
					}
				}
			}
		}
		StringBuilder message = new StringBuilder();
		if(active.isEmpty() && !destroyed.isEmpty()) {
			message.append("All Bounty Targets destroyed. Contract can now be completed.\n");
			stateData.put("completed", true);
		} else if(!active.isEmpty()) {
			message.append("Bounty Target(s) destroyed: ");
			if(destroyed.isEmpty()) {
				message.append("None\n");
			} else {
				for(String name : destroyed) {
					message.append(name).append(", ");
				}
				message.setLength(message.length() - 2);
			}
			message.append(" (").append(destroyed.size()).append(" / ").append(active.size() + destroyed.size()).append(")\n");
		}
		if(message.length() > 0 && changed) {
			// If there are any messages to send, send them to the player.
			playerState.sendServerMessagePlayerInfo(new Object[] {message.toString()});
		}

	public JSONObject spawnTargetMobs() {
		assert getBountyType() == MOB : "BountyContract can only spawn mobs for bounty type MOB.";
		JSONObject spawnedData = new JSONObject();
		JSONArray spawns = new JSONArray();
		if(targetData.has("mob_list")) {
			JSONArray mobList = targetData.getJSONArray("mob_list");
			for(int i = 0; i < mobList.length(); i++) {
				JSONObject mobData = mobList.getJSONObject(i);
				Vector3i sector = getSector();
				int factionId = -1;
				SegmentController controller = BlueprintUtils.spawnAsMob(mobData, sector, factionId);
				if(controller == null) {
					Contracts.getInstance().logWarning("Failed to spawn mob: " + mobData.getString("bp_name") + " in sector " + sector);
					continue;
				}
				JSONObject spawnData = new JSONObject();
				spawnData.put("bp_name", mobData.getString("bp_name"));
				spawnData.put("spawn_name", mobData.getString("spawn_name"));
				spawnData.put("sector", sector);
				spawnData.put("controller_id", controller.getId());
				spawnData.put("status", "active");
				spawns.put(spawnData);
			}
		} else {
			Contracts.getInstance().logWarning("No mobs to spawn for bounty contract.");
		}
		spawnedData.put("sector", getSector());
		spawnedData.put("spawns", spawns);
		return spawnedData;
	}*/
}
