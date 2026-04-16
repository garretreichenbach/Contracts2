package videogoose.contracts.manager;

import api.common.GameCommon;
import api.common.GameServer;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.schine.network.server.ServerMessage;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.contract.ContractData;
import videogoose.contracts.data.contract.EscortContract;
import videogoose.contracts.data.contract.active.ActiveContractData;
import videogoose.contracts.data.contract.active.ActiveContractDataManager;
import videogoose.contracts.utils.BlueprintUtils;
import videogoose.contracts.utils.FlavorUtils;
import videogoose.contracts.utils.SectorUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active escort missions on the server side.
 * Tracks spawned cargo ships, defender escorts, and pirate waves.
 * Runs periodic updates to advance cargo along the route and spawn pirates.
 */
public class EscortManager {

	private static EscortManager instance;

	private final ConcurrentHashMap<String, EscortSession> activeSessions = new ConcurrentHashMap<>();

	public static EscortManager getInstance() {
		if(instance == null) instance = new EscortManager();
		return instance;
	}

	/**
	 * Starts an escort session when a player accepts an escort contract.
	 */
	public void startEscort(EscortContract contract, PlayerState player) {
		String contractUUID = contract.getUUID();
		if(activeSessions.containsKey(contractUUID)) return;
		EscortSession session = new EscortSession(contract, player.getName());
		activeSessions.put(contractUUID, session);
		session.spawnCargoShips();
		session.spawnDefenders(contract.getDifficulty());
		Contracts.getInstance().logInfo("Escort session started for " + player.getName() + " (contract: " + contractUUID + ")");
	}

	/**
	 * Called periodically from a timer to update all active escort sessions.
	 */
	public void update() {
		Iterator<Map.Entry<String, EscortSession>> it = activeSessions.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, EscortSession> entry = it.next();
			EscortSession session = entry.getValue();
			if(session.isFinished()) {
				session.cleanup();
				it.remove();
				continue;
			}
			session.tick();
		}
	}

	/**
	 * Called when a cargo or defender entity is destroyed.
	 */
	public void onEntityDestroyed(SegmentController entity) {
		for(EscortSession session : activeSessions.values()) {
			session.onEntityDestroyed(entity);
		}
	}

	/**
	 * Gets the session for a given contract UUID, if active.
	 */
	public EscortSession getSession(String contractUUID) {
		return activeSessions.get(contractUUID);
	}

	public void removeSession(String contractUUID) {
		EscortSession session = activeSessions.remove(contractUUID);
		if(session != null) session.cleanup();
	}

	/**
	 * Represents a single active escort mission instance.
	 */
	public static class EscortSession {
		private final EscortContract contract;
		private final String playerName;
		private final List<String> cargoEntityNames = new ArrayList<>();
		private final List<String> defenderEntityNames = new ArrayList<>();
		private final Set<String> destroyedCargo = ConcurrentHashMap.newKeySet();
		private int currentWaypointIndex;
		private long lastPirateSpawnTime;
		private long lastUpdateTime;
		private boolean finished;
		private boolean cargoSpawned;

		public EscortSession(EscortContract contract, String playerName) {
			this.contract = contract;
			this.playerName = playerName;
			lastPirateSpawnTime = System.currentTimeMillis();
			lastUpdateTime = System.currentTimeMillis();
		}

		public void spawnCargoShips() {
			Vector3i startSector = contract.getStartSector();
			List<String> cargoBPs = contract.getCargoBlueprintNames();
			int cargoCount = contract.getTotalCargoCount();
			int factionId = FactionManager.TRAIDING_GUILD_ID;
			for(int i = 0; i < cargoCount; i++) {
				String bpName = cargoBPs.get(i % cargoBPs.size());
				String spawnName = FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.TRADERS) + " Cargo-" + (i + 1);
				JSONObject mob = new JSONObject();
				mob.put("bp_name", bpName);
				mob.put("spawn_name", spawnName);
				SegmentController spawned = BlueprintUtils.spawnAsMob(mob, startSector, factionId);
				if(spawned != null) {
					cargoEntityNames.add(spawned.getRealName());
					setEntityTarget(spawned, getNextWaypoint());
				}
			}
			cargoSpawned = true;
			Contracts.getInstance().logInfo("Spawned " + cargoEntityNames.size() + " cargo ships for escort contract " + contract.getUUID());
		}

		public void spawnDefenders(ContractData.Difficulty difficulty) {
			List<String> defenderBPs = contract.getDefenderBlueprintNames();
			if(defenderBPs.isEmpty()) return;
			int defenderCount;
			switch(difficulty) {
				case EASY:
					defenderCount = 3;
					break;
				case NORMAL:
					defenderCount = 2;
					break;
				case HARD:
					defenderCount = 1;
					break;
				default:
					defenderCount = 0;
					break;
			}
			if(defenderCount == 0) return;
			Vector3i startSector = contract.getStartSector();
			int factionId = FactionManager.TRAIDING_GUILD_ID;
			for(int i = 0; i < defenderCount; i++) {
				String bpName = defenderBPs.get(i % defenderBPs.size());
				String spawnName = FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.TRADERS) + " Escort-" + (i + 1);
				JSONObject mob = new JSONObject();
				mob.put("bp_name", bpName);
				mob.put("spawn_name", spawnName);
				SegmentController spawned = BlueprintUtils.spawnAsMob(mob, startSector, factionId);
				if(spawned != null) {
					defenderEntityNames.add(spawned.getRealName());
				}
			}
			Contracts.getInstance().logInfo("Spawned " + defenderEntityNames.size() + " defender escorts");
		}

		public void tick() {
			long now = System.currentTimeMillis();
			if(now - lastUpdateTime < ConfigManager.getEscortUpdateInterval()) return;
			lastUpdateTime = now;

			if(!cargoSpawned || finished) return;

			// Check if player is too far from the escort fleet
			PlayerState player = GameCommon.getPlayerFromName(playerName);
			if(player != null) {
				Vector3i playerSector = player.getCurrentSector();
				Vector3i cargoSector = getLeadCargoSector();
				if(cargoSector != null && !SectorUtils.isInRange(playerSector, cargoSector, 5)) {
					finished = true;
					Contracts.getInstance().logInfo("Escort cancelled for " + playerName + ": player too far from convoy");
					ServerMessage msg = new ServerMessage(new String[]{"Escort mission failed! You moved too far from the convoy."}, ServerMessage.MESSAGE_TYPE_ERROR);
					player.sendServerMessage(msg);
					// Cancel the active contract
					ActiveContractData activeContract = ActiveContractDataManager.getInstance(true).getFromContractUUID(contract.getUUID(), playerName, true);
					if(activeContract != null) {
						ActiveContractDataManager.getInstance(true).completeContract(activeContract, true);
					}
					return;
				}
			}

			// Check if all cargo destroyed
			if(destroyedCargo.size() >= contract.getTotalCargoCount()) {
				finished = true;
				return;
			}

			// Check if cargo reached current waypoint, advance to next
			boolean allAtWaypoint = true;
			Vector3i targetWaypoint = getCurrentWaypoint();
			for(String cargoName : cargoEntityNames) {
				if(destroyedCargo.contains(cargoName)) continue;
				SegmentController cargo = getEntityByName(cargoName);
				if(cargo == null) continue;
				if(!SectorUtils.isInRange(cargo.getSector(new Vector3i()), targetWaypoint, 1)) {
					allAtWaypoint = false;
					break;
				}
			}

			if(allAtWaypoint && currentWaypointIndex < contract.getRoute().size() - 1) {
				currentWaypointIndex++;
				Vector3i nextWaypoint = getCurrentWaypoint();
				for(String cargoName : cargoEntityNames) {
					if(destroyedCargo.contains(cargoName)) continue;
					SegmentController cargo = getEntityByName(cargoName);
					if(cargo != null) setEntityTarget(cargo, nextWaypoint);
				}
				for(String defName : defenderEntityNames) {
					SegmentController defender = getEntityByName(defName);
					if(defender != null) setEntityTarget(defender, nextWaypoint);
				}
			}

			// Check route completion
			if(currentWaypointIndex >= contract.getRoute().size() - 1 && allAtWaypoint) {
				finished = true;
				contract.setRouteComplete(true);
				ActiveContractData activeContract = ActiveContractDataManager.getInstance(true).getFromContractUUID(contract.getUUID(), playerName, true);
				if(activeContract != null) {
					activeContract.setCanComplete(true, true);
				}
				if(player != null) {
					int surviving = contract.getCargoSurviving();
					ServerMessage msg = new ServerMessage(new String[]{"Escort complete! " + surviving + "/" + contract.getTotalCargoCount() + " cargo ships survived."}, ServerMessage.MESSAGE_TYPE_INFO);
					player.sendServerMessage(msg);
				}
				return;
			}

			// Spawn pirate waves
			if(now - lastPirateSpawnTime >= ConfigManager.getEscortPirateWaveInterval()) {
				spawnPirateWave();
				lastPirateSpawnTime = now;
			}
		}

		private void spawnPirateWave() {
			// Find a living cargo ship to spawn pirates near
			Vector3i spawnSector = null;
			for(String cargoName : cargoEntityNames) {
				if(destroyedCargo.contains(cargoName)) continue;
				SegmentController cargo = getEntityByName(cargoName);
				if(cargo != null) {
					spawnSector = cargo.getSector(new Vector3i());
					break;
				}
			}
			if(spawnSector == null) return;

			Random random = new Random();
			int pirateCount = random.nextInt(ConfigManager.getEscortPirateMaxPerWave()) + 1;
			HashMap<org.schema.game.server.data.blueprintnw.BlueprintEntry, Float> pirateWeights = BlueprintUtils.getPirateSpawnWeights();
			if(pirateWeights.isEmpty()) return;
			ArrayList<org.schema.game.server.data.blueprintnw.BlueprintEntry> pirateBPs = new ArrayList<>(pirateWeights.keySet());

			int spawned = 0;
			for(int i = 0; i < pirateCount; i++) {
				org.schema.game.server.data.blueprintnw.BlueprintEntry bp = pirateBPs.get(random.nextInt(pirateBPs.size()));
				if(random.nextFloat() > pirateWeights.get(bp)) continue;
				JSONObject mob = new JSONObject();
				mob.put("bp_name", bp.getName());
				mob.put("spawn_name", FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.PIRATE) + " Wave-" + System.currentTimeMillis() % 10000);
				SegmentController pirate = BlueprintUtils.spawnAsMob(mob, spawnSector, FactionManager.PIRATES_ID);
				if(pirate != null) spawned++;
			}
			if(spawned > 0) {
				Contracts.getInstance().logInfo("Spawned " + spawned + " pirates for escort contract " + contract.getUUID());
			}
		}

		public void onEntityDestroyed(SegmentController entity) {
			String name = entity.getRealName();
			if(cargoEntityNames.contains(name) && !destroyedCargo.contains(name)) {
				destroyedCargo.add(name);
				contract.onCargoDestroyed(true);
				Contracts.getInstance().logInfo("Cargo ship destroyed: " + name + " (" + contract.getCargoSurviving() + " remaining)");
				PlayerState player = GameCommon.getPlayerFromName(playerName);
				if(player != null) {
					ServerMessage msg = new ServerMessage(new String[]{"A cargo ship has been destroyed! " + contract.getCargoSurviving() + "/" + contract.getTotalCargoCount() + " remaining."}, ServerMessage.MESSAGE_TYPE_WARNING);
					player.sendServerMessage(msg);
				}
			}
		}

		public void cleanup() {
			// Remove surviving cargo and defender entities
			for(String name : cargoEntityNames) {
				if(destroyedCargo.contains(name)) continue;
				SegmentController entity = getEntityByName(name);
				if(entity != null) entity.markForPermanentDelete(true);
			}
			for(String name : defenderEntityNames) {
				SegmentController entity = getEntityByName(name);
				if(entity != null) entity.markForPermanentDelete(true);
			}
		}

		public boolean isFinished() {
			return finished;
		}

		private Vector3i getLeadCargoSector() {
			for(String cargoName : cargoEntityNames) {
				if(destroyedCargo.contains(cargoName)) continue;
				SegmentController cargo = getEntityByName(cargoName);
				if(cargo != null) return cargo.getSector(new Vector3i());
			}
			return null;
		}

		private Vector3i getCurrentWaypoint() {
			return contract.getRoute().get(Math.min(currentWaypointIndex, contract.getRoute().size() - 1));
		}

		private Vector3i getNextWaypoint() {
			int next = Math.min(currentWaypointIndex + 1, contract.getRoute().size() - 1);
			return contract.getRoute().get(next);
		}

		private void setEntityTarget(SegmentController entity, Vector3i sector) {
			try {
				if(entity instanceof Ship) {
					Ship ship = (Ship) entity;
					TargetProgram<?> program = (TargetProgram<?>) ship.getAiConfiguration().getAiEntityState().getCurrentProgram();
					program.setSectorTarget(new Vector3i(sector));
				}
			} catch(Exception e) {
				Contracts.getInstance().logException("Failed to set sector target for " + entity.getRealName(), e);
			}
		}

		private SegmentController getEntityByName(String name) {
			try {
				return GameServer.getServerState().getSegmentControllersByName().get(name);
			} catch(Exception e) {
				return null;
			}
		}

		public String getPlayerName() {
			return playerName;
		}

		public EscortContract getContract() {
			return contract;
		}
	}
}
