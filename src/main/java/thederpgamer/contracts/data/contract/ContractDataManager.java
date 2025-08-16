package thederpgamer.contracts.data.contract;

import api.common.GameClient;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.faction.FactionManager;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.DataManager;
import thederpgamer.contracts.data.SerializableData;
import thederpgamer.contracts.data.player.PlayerData;

import java.util.*;

import static thederpgamer.contracts.gui.contract.newcontract.NewContractPanel.getProductionFilter;

public class ContractDataManager extends DataManager<ContractData> {

	private static ContractDataManager instance;
	private final Set<ContractData> clientCache = new HashSet<>();

	public static ContractDataManager getInstance(boolean server) {
		if(instance == null) {
			instance = new ContractDataManager();
			if(!server) {
				instance.requestFromServer();
			}
		}
		return instance;
	}

	public static void completeContract(PlayerData playerData, ContractData contract) {
		ContractDataManager dataManager = getInstance(playerData.getPlayerState().isOnServer());
		playerData.removeContract(contract.getUUID());
		dataManager.removeData(contract, playerData.getPlayerState().isOnServer());
		contract.onCompletion(playerData.getPlayerState());
		playerData.sendMail(contract.getContractor().getName(), "Contract Completion", "You have completed the contract \"" + contract.getName() + "\"\nand have been rewarded " + contract.getReward() + " credits!");
		dataManager.sendPacket(contract, REMOVE_DATA, !playerData.getPlayerState().isOnServer());
	}

	@Override
	public Collection<ContractData> getServerCache() {
		ArrayList<ContractData> serverCache = new ArrayList<>();
		serverCache.addAll(PersistentObjectUtil.getCopyOfObjects(Contracts.getInstance().getSkeleton(), BountyContract.class));
		serverCache.addAll(PersistentObjectUtil.getCopyOfObjects(Contracts.getInstance().getSkeleton(), ItemsContract.class));
		return serverCache;
	}

	@Override
	public SerializableData.DataType getDataType() {
		return SerializableData.DataType.CONTRACT_DATA;
	}

	@Override
	public Set<ContractData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(ContractData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(ContractData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(ContractData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	@Override
	public void createMissingData(Object... args) {

	}

	public void generateRandomContract() {
		Random random = new Random();
		ContractData.ContractType contractType = ContractData.ContractType.getRandomType();
		ArrayList<Short> possibleIDs = new ArrayList<>();
		String contractName;
		int amountInt = random.nextInt(3000 - 100) + 100;
		long basePrice;
		short targetId;
		int targetAmount;
		ContractData randomContract = null;
		switch(contractType) {
			case ITEMS:
				for(ElementInformation info : getProductionFilter()) {
					possibleIDs.add(info.getId());
				}
				int productionIndex = random.nextInt(possibleIDs.size() - 1) + 1;
				short productionID = possibleIDs.get(productionIndex);
				contractName = "Produce x" + amountInt + " " + ElementKeyMap.getInfo(productionID).getName();
				basePrice = (long) Math.abs(ElementKeyMap.getInfo(productionID).getPrice(true) * 1.3f); // Increase base price by 30% for contract reward calculation
				long reward = Math.abs((basePrice * amountInt));
				targetId = productionID;
				targetAmount = amountInt;
				randomContract = new ItemsContract(FactionManager.TRAIDING_GUILD_ID, contractName, reward, targetId, targetAmount);
				break;
			case BOUNTY:
				randomContract = BountyContract.generateRandomMob(FactionManager.TRAIDING_GUILD_ID);
				break;
		}
		if(randomContract != null) {
			addData(randomContract, true);
		}
	}

	public ArrayList<? extends ContractData> getContractsOfType(Class<? extends ContractData> contractType, boolean isServer) {
		ArrayList<ContractData> contracts = new ArrayList<>();
		for(ContractData contract : getCache(isServer)) {
			if(contractType.isInstance(contract)) {
				contracts.add(contract);
			}
		}
		return contracts;
	}

	public boolean canCompleteAny() {
		for(ContractData contract : getCache(false)) {
			if(contract.canComplete(GameClient.getClientPlayerState())) {
				return true;
			}
		}
		return false;
	}
}
