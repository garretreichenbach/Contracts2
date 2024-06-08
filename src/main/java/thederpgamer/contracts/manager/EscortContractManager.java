package thederpgamer.contracts.manager;

import api.common.GameCommon;
import api.utils.StarRunnable;
import api.utils.other.HashList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.escort.EscortContract;
import thederpgamer.contracts.data.contract.escort.EscortShipData;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.networking.server.ServerDataManager;
import thederpgamer.contracts.utils.BlueprintUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class EscortContractManager {

	private static final HashMap<PlayerData, HashMap<EscortContract, Fleet>> activeContracts = new HashMap<>();
	private static final HashList<PlayerData, EscortContract> spawnQueue = new HashList<>();

	public static void initialize() {
		(new StarRunnable() {
			@Override
			public void run() {
				ArrayList<PlayerData> toRemove = new ArrayList<>();
				for(PlayerData player : spawnQueue.keySet()) {
					if(player.getPlayerState() != null) {
						List<EscortContract> contracts = spawnQueue.get(player);
						for(EscortContract contract : contracts) {
							if(contract.getCargoData().getStartSector().equals(player.getPlayerState().getCurrentSector())) {
								Vector3i sector = contract.getCargoData().getStartSector();
								if(player.getPlayerState().getCurrentSector().equals(sector)) toRemove.add(player);
							}
						}
					}
				}
				for(PlayerData player : toRemove) {
					ArrayList<EscortContract> contracts = new ArrayList<>(spawnQueue.get(player));
					for(EscortContract contract : contracts) {
						spawnQueue.get(player).remove(contract);
						addToActive(player, contract);
					}
				}
			}
		}).runTimer(Contracts.getInstance(), 100);
	}

	public static void addToActive(PlayerData player, EscortContract contract) {
		if(isActiveFor(player, contract)) return;
		HashMap<EscortContract, Fleet> map = new HashMap<>();
		NPCFaction traders = getTradersFaction();
		ElementCountMap countMap = contract.getCargoData().toCountMap();
		Fleet fleet = traders.getFleetManager().spawnTradingFleet(countMap, contract.getCargoData().getStartSector(), contract.getCargoData().getEndSector());
		fleet.setCurrentMoveTarget(contract.getCargoData().getEndSector());
		map.put(contract, fleet);
		activeContracts.put(player, map);
		spawnQueue.remove(player);
	}

	public static NPCFaction getTradersFaction() {
		return (NPCFaction) GameCommon.getGameState().getFactionManager().getFaction(FactionManager.NPC_FACTION_START);
	}

	public static void removeFromActive(PlayerData player, EscortContract contract) {
		if(!isActiveFor(player, contract)) return;
		activeContracts.get(player).remove(contract);
	}

	public static boolean update(PlayerData player, EscortContract contract) {
		Fleet fleet = activeContracts.get(player).get(contract);
		boolean continueLoop = contract.updateRunner(player.getPlayerState(), fleet.getMembers());
		if(!continueLoop) {
			if(contract.canComplete(ServerDataManager.getPlayerState(player))) {
				long finalReward = contract.getCargoData().getReward();
				for(FleetMember member : fleet.getMembers()) {
					if(!member.isLoaded()) {
						//Cancel the contract if any of the ships are not loaded
						ServerDataManager.failContract(player, contract);
						return false;
					} else {
						float hp = member.getShipPercent();
						BlueprintEntry entry = BlueprintUtils.getBlueprint(member.getLoaded().blueprintIdentifier);
						if(entry != null) {
							long shipWorth = entry.getPrice();
							//If the ship is damaged, reduce the reward
							if(hp < 1.0) {
								double percent = hp * 100.0;
								double reduction = (percent / 100.0) * shipWorth;
								finalReward -= (long) reduction;
							}
						}
					}
				}
				finalReward = Math.min(0, finalReward);
				contract.setReward(finalReward);
				ServerDataManager.completeContract(player, contract);
			} else ServerDataManager.failContract(player, contract);
			return false;
		} else return true;
	}

	public static boolean isActiveFor(PlayerData player, EscortContract contract) {
		return isAnyActive(player) && activeContracts.get(player).containsKey(contract);
	}

	public static boolean isAnyActive(PlayerData player) {
		return activeContracts.containsKey(player);
	}

	public static void addToQueue(PlayerData player, EscortContract contract) {
		spawnQueue.add(player, contract);
	}
}