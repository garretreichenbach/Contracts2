package thederpgamer.contracts.networking.client;

import api.common.GameClient;
import thederpgamer.contracts.ConfigManager;
import thederpgamer.contracts.GUIManager;
import thederpgamer.contracts.data.contract.Contract;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ClientDataManager {

	private static final HashMap<String, Contract> clientData = new HashMap<>();

	public static void updateClientTimer(String contractUID, long timeRemaining) {
		Contract contract = clientData.get(contractUID);
		if(contract != null) {
			contract.getClaimants().put(GameClient.getClientPlayerState().getName(), timeRemaining);
			GUIManager.getInstance().contractsTab.flagForRefresh();
		}
	}

	public static ArrayList<Contract> getClaimedContracts() {
		ArrayList<Contract> claimedContracts = new ArrayList<>();
		for(Contract contract : clientData.values()) {
			if(contract.getClaimants().containsKey(GameClient.getClientPlayerState().getName())) claimedContracts.add(contract);
		}
		return claimedContracts;
	}

	public static void updateClientData(String contractUID, Contract data) {
		clientData.put(contractUID, data);
		GUIManager.getInstance().contractsTab.flagForRefresh();
	}

	public static void addContract(Contract contractData) {
		clientData.put(contractData.getUID(), contractData);
		if(GUIManager.getInstance().contractsTab != null) GUIManager.getInstance().contractsTab.flagForRefresh();
	}

	public static void removeClientData(String contractUID) {
		clientData.remove(contractUID);
		GUIManager.getInstance().contractsTab.flagForRefresh();
	}

	public static Contract getClientData(String contractUID) {
		return clientData.get(contractUID);
	}

	public static HashMap<String, Contract> getClientData() {
		return clientData;
	}

	public static void claimContract(String uid) {
		Contract contract = clientData.get(uid);
		if(contract != null) {
			contract.getClaimants().put(GameClient.getClientPlayerState().getName(), ConfigManager.getMainConfig().getLong("contract-timer-max"));
			ClientActionType.CLAIM_CONTRACT.send(uid);
			GUIManager.getInstance().contractsTab.flagForRefresh();
		}
	}

	public static boolean canCompleteAny() {
		for(Contract contract : clientData.values()) {
			if(contract.canComplete(GameClient.getClientPlayerState())) return true;
		}
		return false;
	}
}
