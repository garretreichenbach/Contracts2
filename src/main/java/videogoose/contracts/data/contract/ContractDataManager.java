package videogoose.contracts.data.contract;

import api.mod.config.PersistentObjectUtil;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.SerializableData;
import videogoose.contracts.data.player.PlayerData;
import videogoose.contracts.manager.ConfigManager;
import videogoose.contracts.utils.SectorUtils;

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

	public static void completeContract(PlayerData playerData, ContractData contract) {
		PlayerState player = playerData.getPlayerState();
		if(player == null || !contract.canComplete(player)) return;
		contract.onCompletion(player);
		playerData.removeContract(contract.getUUID());
		getInstance(player.isOnServer()).removeData(contract, player.isOnServer());
	}

	@Override
	public Collection<ContractData> getServerCache() {
		ArrayList<ContractData> serverCache = new ArrayList<>();
		serverCache.addAll(PersistentObjectUtil.getCopyOfObjects(Contracts.getInstance().getSkeleton(), BountyContract.class));
		serverCache.addAll(PersistentObjectUtil.getCopyOfObjects(Contracts.getInstance().getSkeleton(), ItemsContract.class));
		serverCache.addAll(PersistentObjectUtil.getCopyOfObjects(Contracts.getInstance().getSkeleton(), EscortContract.class));
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
			case ESCORT:
				randomContract = generateRandomEscort(difficulty);
				break;
		}
		if(randomContract != null) {
			addData(randomContract, true);
		}
	}

	private EscortContract generateRandomEscort(ContractData.Difficulty difficulty) {
		Random random = new Random();
		int routeLength = ConfigManager.getEscortRouteMinLength() + random.nextInt(ConfigManager.getEscortRouteMaxLength() - ConfigManager.getEscortRouteMinLength() + 1);
		List<Vector3i> route = new ArrayList<>();
		Vector3i current = SectorUtils.getRandomSector(10);
		route.add(current);
		for(int i = 1; i < routeLength; i++) {
			// Each waypoint is 2-4 sectors away from the previous
			Vector3i next;
			int attempts = 0;
			do {
				int dx = (random.nextInt(3) + 2) * (random.nextBoolean() ? 1 : -1);
				int dy = (random.nextInt(3)) * (random.nextBoolean() ? 1 : -1);
				int dz = (random.nextInt(3) + 2) * (random.nextBoolean() ? 1 : -1);
				next = new Vector3i(current.x + dx, current.y + dy, current.z + dz);
				attempts++;
			} while(SectorUtils.tooCloseToStar(next) && attempts < 20);
			route.add(next);
			current = next;
		}

		int cargoCount = ConfigManager.getEscortCargoCount();
		String[] cargoBPPool = ConfigManager.getEscortCargoBlueprintPool();
		List<String> cargoBPs = new ArrayList<>();
		if(cargoBPPool.length > 0) {
			for(String bp : cargoBPPool) cargoBPs.add(bp.trim());
		} else {
			cargoBPs.add("T180-18");
		}

		String[] defenderBPPool = ConfigManager.getEscortDefenderBlueprintPool();
		List<String> defenderBPs = new ArrayList<>();
		for(String bp : defenderBPPool) {
			String trimmed = bp.trim();
			if(!trimmed.isEmpty()) defenderBPs.add(trimmed);
		}

		Vector3i dest = route.get(route.size() - 1);
		String contractName = "[" + difficulty.displayName + "] Escort " + cargoCount + " cargo ships to Sector " + dest;

		return new EscortContract(FactionManager.TRAIDING_GUILD_ID, contractName, ConfigManager.getEscortBaseReward(), difficulty, route, cargoBPs, defenderBPs, cargoCount);
	}

	public boolean canCompleteAny(PlayerState player) {
		return getClientCache().stream().anyMatch(contract -> contract.canComplete(player));
	}

	public List<? extends ContractData> getContractsOfType(Class<? extends ContractData> contractType, boolean isServer) {
		return getCache(isServer).stream().filter(contractType::isInstance).collect(Collectors.toList());
	}
}
