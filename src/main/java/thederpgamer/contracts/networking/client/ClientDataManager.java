package thederpgamer.contracts.networking.client;

import thederpgamer.contracts.GUIManager;
import thederpgamer.contracts.data.contract.ClientContractData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ClientDataManager {

	private static final HashMap<String, ClientContractData> clientData = new HashMap<>();

	public static void updateClientTimer(String contractUID, long timeRemaining) {
		ClientContractData contract = clientData.get(contractUID);
		if(contract != null) {
			contract.setTimeRemaining(timeRemaining);
			GUIManager.getInstance().contractsTab.flagForRefresh();
		}
	}

	public static ArrayList<ClientContractData> getClaimedContracts() {
		ArrayList<ClientContractData> claimedContracts = new ArrayList<>();
		for(ClientContractData contract : clientData.values()) {
			if(contract.isClaimed()) claimedContracts.add(contract);
		}
		return claimedContracts;
	}

	public static void setCanComplete(String contractUID) {
		ClientContractData contract = clientData.get(contractUID);
		if(contract != null) {
			contract.setCanComplete(true);
			GUIManager.getInstance().contractsTab.flagForRefresh();
		}
	}

	public static void updateClientData(String contractUID, ClientContractData data) {
		clientData.put(contractUID, data);
		GUIManager.getInstance().contractsTab.flagForRefresh();
	}

	public static void addContract(ClientContractData contractData) {
		clientData.put(contractData.getUID(), contractData);
		if(GUIManager.getInstance().contractsTab != null) GUIManager.getInstance().contractsTab.flagForRefresh();
	}

	public static void removeClientData(String contractUID) {
		clientData.remove(contractUID);
		GUIManager.getInstance().contractsTab.flagForRefresh();
	}

	public static ClientContractData getClientData(String contractUID) {
		return clientData.get(contractUID);
	}

	public static HashMap<String, ClientContractData> getClientData() {
		return clientData;
	}

	public static void claimContract(String uid) {
		ClientContractData contract = clientData.get(uid);
		if(contract != null) {
			contract.setClaimed(true);
			ClientActionType.CLAIM_CONTRACT.send(uid);
			GUIManager.getInstance().contractsTab.flagForRefresh();
		}
	}

	public static boolean canCompleteAny() {
		for(ClientContractData contract : clientData.values()) {
			if(contract.canComplete()) return true;
		}
		return false;
	}
}
