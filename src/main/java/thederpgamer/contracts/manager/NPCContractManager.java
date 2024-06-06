package thederpgamer.contracts.manager;

import api.common.GameCommon;
import api.utils.other.HashList;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import thederpgamer.contracts.data.contract.ActiveContractRunnable;
import thederpgamer.contracts.data.contract.BountyContract;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class NPCContractManager {

	private static final HashMap<Integer, NPCFaction> npcFactions = new HashMap<>();
	private static final HashMap<PlayerData, HashList<Contract, SegmentController>> activeContracts = new HashMap<>();

	public static void initialize() {
		for(String i : ConfigManager.getMainConfig().getList("npc-factions")) {
			int id = Integer.parseInt(i);
			npcFactions.put(id, (NPCFaction) GameCommon.getGameState().getFactionManager().getFaction(id));
		}
		//Todo: Do something with the npcFactions once StarLoader has the new events
	}

	public static void addToActive(PlayerData player, Contract contract) {
		if(isActiveFor(player, contract)) return;
		HashList<Contract, SegmentController> map = new HashList<>();
		List<?> data = getActiveRunnable(contract).startRunner(player.getPlayerState());
		for(Object o : data) {
			if(o instanceof SegmentController) map.add(contract, (SegmentController) o);
		}
		activeContracts.put(player, map);
	}

	public static void removeFromActive(PlayerData player, Contract contract) {
		if(!isActiveFor(player, contract)) return;
		activeContracts.get(player).remove(contract);
	}

	public static boolean update(PlayerData player, Contract contract) {
		ActiveContractRunnable runnable = getActiveRunnable(contract);
		List<?> data = activeContracts.get(player).get(contract);
		if(!runnable.updateRunner(player.getPlayerState(), data)) {
			ServerDataManager.completeContract(player, contract);
			activeContracts.get(player).remove(contract);
			return false;
		} else return true;
	}

	public static boolean isActiveFor(PlayerData player, Contract contract) {
		return isAnyActive(player) && activeContracts.get(player).containsKey(contract);
	}

	public static boolean isAnyActive(PlayerData player) {
		return activeContracts.containsKey(player);
	}

	public static ActiveContractRunnable getActiveRunnable(Contract contract) {
		return (ActiveContractRunnable) contract;
	}

	public static void removeActiveClaims() {
		ArrayList<Contract> contracts = ServerDataManager.getAllContracts();
		for(Contract contract : contracts) {
			if(contract instanceof BountyContract && ((BountyContract) contract).getTargetType() == BountyContract.BountyTargetType.NPC) {
				contract.getClaimants().clear();
				ServerDataManager.addOrUpdateContract(contract);
				//Remove claims for "active" contracts cus it would be too hard to track them across restarts
			}
		}
	}
}