package thederpgamer.contracts.data.contract.escort;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.contract.ActiveContractRunnable;
import thederpgamer.contracts.data.contract.Contract;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class EscortContract extends Contract implements ActiveContractRunnable {

	private EscortCargoData cargoData;
	private boolean completed;

	public EscortContract(int contractorID, String name, long reward, EscortCargoData cargoData) {
		super(contractorID, name, reward);
		this.cargoData = cargoData;
	}

	public EscortContract(JSONObject json) {
		super(json);
		fromJSON(json);
	}

	public EscortContract(PacketReadBuffer packetReadBuffer) throws IOException {
		super(packetReadBuffer);
		readFromBuffer(packetReadBuffer);
	}

	@Override
	public boolean canStartRunner(PlayerState player) {
		return true;
	}

	@Override
	public List<SegmentController> startRunner(PlayerState player) {
		return Collections.emptyList();
	}

	@Override
	public boolean updateRunner(PlayerState player, List<?> data) {
		//Check if all ships have reached the end sector
		for(Object object : data) {
			FleetMember fleetMember = (FleetMember) object;
			if(!fleetMember.getSector().equals(cargoData.getEndSector())) {
				completed = false;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canComplete(PlayerState player) {
		return completed;
	}

	@Override
	public ContractType getContractType() {
		return ContractType.ESCORT;
	}

	@Override
	public void onCompletion(PlayerState player) {
		assert player.isOnServer();
		player.setCredits(player.getCredits() + getReward());
	}

	@Override
	public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
		super.readFromBuffer(readBuffer);
		cargoData = new EscortCargoData(readBuffer);
		completed = readBuffer.readBoolean();
	}

	@Override
	public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
		super.writeToBuffer(writeBuffer);
		cargoData.writeToBuffer(writeBuffer);
		writeBuffer.writeBoolean(completed);
	}

	@Override
	public void fromJSON(JSONObject json) {
		super.fromJSON(json);
		cargoData = new EscortCargoData(json.getJSONObject("cargoData"));
		completed = json.getBoolean("completed");
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("cargoData", cargoData.toJSON());
		json.put("completed", completed);
		return json;
	}

	public EscortCargoData getCargoData() {
		return cargoData;
	}
}
