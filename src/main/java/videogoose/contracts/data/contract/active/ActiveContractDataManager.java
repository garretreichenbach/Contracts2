package videogoose.contracts.data.contract.active;

import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.SerializableData;
import videogoose.contracts.data.contract.ContractData;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.data.contract.EscortContract;
import videogoose.contracts.data.player.PlayerData;
import videogoose.contracts.data.player.PlayerDataManager;
import videogoose.contracts.manager.ConfigManager;
import videogoose.contracts.manager.EscortManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActiveContractDataManager extends DataManager<ActiveContractData> {

	private static ActiveContractDataManager instance;
	private final Set<ActiveContractData> clientCache = ConcurrentHashMap.newKeySet();

	public static ActiveContractDataManager getInstance(boolean server) {
		if(instance == null) {
			instance = new ActiveContractDataManager();
			if(!server) instance.requestFromServer();
		}
		return instance;
	}

	@Override
	public Set<ActiveContractData> getServerCache() {
		return PersistentObjectUtil.getObjects(Contracts.getInstance().getSkeleton(), ActiveContractData.class).stream().map(o -> (ActiveContractData) o).collect(Collectors.toSet());
	}

	@Override
	public SerializableData.DataType getDataType() {
		return SerializableData.DataType.ACTIVE_CONTRACT_DATA;
	}

	@Override
	public Set<ActiveContractData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(ActiveContractData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(ActiveContractData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(ActiveContractData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	@Override
	public void createMissingData(Object... args) {

	}

	public boolean acceptContract(ContractData contract, PlayerState player) {
		boolean server = player.isOnServer();
		PlayerData playerData = PlayerDataManager.getInstance(server).getFromName(player.getName(), server);
		if(playerData == null) return false;
		if(playerData.getContracts().size() >= ConfigManager.getClientMaxActiveContracts()) return false;
		if(playerData.getContracts().contains(contract.getUUID())) return false;
		if(contract instanceof EscortContract) {
			EscortContract escort = (EscortContract) contract;
			if(!player.getCurrentSector().equals(escort.getStartSector())) return false;
		}
		ActiveContractData activeContract = new ActiveContractData(contract, player.getName());
		addData(activeContract, server);
		playerData.getContracts().add(contract.getUUID());
		PlayerDataManager.getInstance(server).updateData(playerData, server);
		contract.getClaimants().put(player.getName(), System.currentTimeMillis());
		ContractDataManager.getInstance(server).updateData(contract, server);
		if(server && contract instanceof EscortContract) {
			EscortManager.getInstance().startEscort((EscortContract) contract, player);
		}
		return true;
	}

	public void completeContract(ActiveContractData activeContract, boolean server) {
		ContractData contract = activeContract.getTargetContract(server);
		if(contract == null) return;
		PlayerData playerData = PlayerDataManager.getInstance(server).getFromName(activeContract.getClaimer(), server);
		if(playerData == null) return;
		if(contract instanceof EscortContract) {
			EscortManager.getInstance().removeSession(contract.getUUID());
		}
		ContractDataManager.completeContract(playerData, contract);
		removeData(activeContract, server);
	}

	public List<ActiveContractData> getContractsForPlayer(String playerName, boolean server) {
		return getCache(server).stream().filter(data -> data.getClaimer().equals(playerName)).collect(Collectors.toList());
	}

	public ActiveContractData getFromContractUUID(String contractUUID, String playerName, boolean server) {
		return getCache(server).stream().filter(data -> data.getTargetContractID().equals(contractUUID) && data.getClaimer().equals(playerName)).findFirst().orElse(null);
	}
}