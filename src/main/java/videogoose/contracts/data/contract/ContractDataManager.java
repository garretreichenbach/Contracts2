package videogoose.contracts.data.contract;

import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.SerializableData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static videogoose.contracts.gui.contract.newcontract.NewContractPanel.getProductionFilter;

public class ContractDataManager extends DataManager<ContractData> {

	private static ContractDataManager instance;
	private final Set<ContractData> clientCache = ConcurrentHashMap.newKeySet();

	public static ContractDataManager getInstance(boolean server) {
		if(instance == null) {
			instance = new ContractDataManager();
			if(!server) {
				instance.requestFromServer();
			}
		}
		return instance;
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
		ContractData.Difficulty difficulty = ContractData.Difficulty.getRandomDifficulty();
		switch(contractType) {
			case ITEMS:
				for(ElementInformation info : getProductionFilter()) {
					possibleIDs.add(info.getId());
				}
				int productionIndex = random.nextInt(possibleIDs.size() - 1) + 1;
				short productionID = possibleIDs.get(productionIndex);
				contractName = "[" + difficulty.displayName + "] Produce x" + amountInt + " " + ElementKeyMap.getInfo(productionID).getName();
				basePrice = (long) Math.abs(ElementKeyMap.getInfo(productionID).getPrice(true) * 1.3f); // Increase base price by 30% for contract reward calculation
				long reward = Math.abs((basePrice * amountInt));
				targetId = productionID;
				targetAmount = amountInt;
				randomContract = new ItemsContract(FactionManager.TRAIDING_GUILD_ID, contractName, reward, targetId, targetAmount, difficulty);
				break;
			case BOUNTY:
				randomContract = BountyContract.generateRandomMob(FactionManager.TRAIDING_GUILD_ID);
				break;
		}
		if(randomContract != null) {
			addData(randomContract, true);
		}
	}

	public static void completeContract(videogoose.contracts.data.player.PlayerData playerData, ContractData contract) {
		org.schema.game.common.data.player.PlayerState player = playerData.getPlayerState();
		if(player == null || !contract.canComplete(player)) return;
		contract.onCompletion(player);
		playerData.removeContract(contract.getUUID());
		ContractDataManager.getInstance(player.isOnServer()).removeData(contract, player.isOnServer());
	}

	public boolean canCompleteAny(PlayerState player) {
		return getClientCache().stream().anyMatch(contract -> contract.canComplete(player));
	}

	public List<? extends ContractData> getContractsOfType(Class<? extends ContractData> contractType, boolean isServer) {
		return getCache(isServer).stream()
				.filter(contractType::isInstance)
				.collect(Collectors.toList());
	}
}
