package thederpgamer.contracts.networking.client;

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
        if(contract != null) contract.setTimeRemaining(timeRemaining);
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
        if(contract != null) contract.setCanComplete(true);
    }

    public static void updateClientData(String contractUID, ClientContractData data) {
        clientData.put(contractUID, data);
    }

    public static void addContract(ClientContractData contractData) {
        clientData.put(contractData.getUID(), contractData);
    }

    public static void removeClientData(String contractUID) {
        clientData.remove(contractUID);
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
        }
    }
}
