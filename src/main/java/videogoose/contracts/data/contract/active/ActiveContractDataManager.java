package videogoose.contracts.data.contract.active;

import api.mod.config.PersistentObjectUtil;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.DataManager;
import videogoose.contracts.data.SerializableData;
import videogoose.contracts.data.contract.ContractData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActiveContractDataManager extends DataManager<ActiveContractData> {

	private final Set<ActiveContractData> clientCache = ConcurrentHashMap.newKeySet();
	private static ActiveContractDataManager instance;

	public static ActiveContractDataManager getInstance(boolean server) {
		if(instance == null) {
			instance = new ActiveContractDataManager();
			if(!server) instance.requestFromServer();
		}
		return instance;
	}

	@Override
	public Set<ActiveContractData> getServerCache() {
		return PersistentObjectUtil.getObjects(Contracts.getInstance().getSkeleton(), ActiveContractData.class)
				.stream().map(o -> (ActiveContractData) o).collect(Collectors.toSet());
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

	public void acceptContract(ContractData contract) {

	}
}