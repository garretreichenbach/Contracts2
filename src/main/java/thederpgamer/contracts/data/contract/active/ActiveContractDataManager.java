package thederpgamer.contracts.data.contract.active;

import api.mod.config.PersistentObjectUtil;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.DataManager;
import thederpgamer.contracts.data.SerializableData;
import thederpgamer.contracts.data.contract.ContractData;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActiveContractDataManager extends DataManager<ActiveContractData> {

	private final Set<ActiveContractData> clientCache = new HashSet<>();
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
		List<Object> objects = PersistentObjectUtil.getObjects(Contracts.getInstance().getSkeleton(), ActiveContractData.class);
		Set<ActiveContractData> data = new HashSet<>();
		for(Object object : objects) data.add((ActiveContractData) object);
		return data;
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