package thederpgamer.contracts.manager;

import api.utils.StarRunnable;
import api.utils.other.HashList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
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

	private static final HashMap<PlayerData, HashList<EscortContract, SegmentController>> activeContracts = new HashMap<>();
	private static final HashList<PlayerData, EscortContract> spawnQueue = new HashList<>();

	public static void initialize() {
		(new StarRunnable() {
			@Override
			public void run() {
				ArrayList<PlayerData> toRemove = new ArrayList<>();
				for(PlayerData player : spawnQueue.keySet()) {
					if(player.getPlayerState() != null){
						List<EscortContract> contracts = spawnQueue.get(player);
						for(EscortContract contract : contracts) {
							if(contract.canStartRunner(player.getPlayerState())) {
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
		HashList<EscortContract, SegmentController> map = new HashList<>();
		List<?> data = contract.startRunner(player.getPlayerState());
		for(Object o : data) {
			if(o instanceof SegmentController) map.add(contract, (SegmentController) o);
		}
		activeContracts.put(player, map);
		spawnQueue.remove(player);
	}

	public static void removeFromActive(PlayerData player, EscortContract contract) {
		if(!isActiveFor(player, contract)) return;
		activeContracts.get(player).remove(contract);
	}

	public static boolean update(PlayerData player, EscortContract contract) {
		List<?> data = activeContracts.get(player).get(contract);
		boolean continueLoop = contract.updateRunner(player.getPlayerState(), data);
		if(!continueLoop) {
			if(contract.canComplete(ServerDataManager.getPlayerState(player))) {
				long finalReward = contract.getReward();
				for(Object obj : data) {
					if(obj instanceof SegmentController) {
						SegmentController ship = (SegmentController) obj;
						double hp = ship.getHpController().getHpPercent();
						EscortShipData shipData = contract.getCargoData().getShipData(ship);
						if(shipData != null) {
							BlueprintEntry entry = BlueprintUtils.getBlueprint(ship.blueprintIdentifier);
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