package videogoose.contracts.networking;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.server.ServerMessage;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.contract.ContractData;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.data.contract.active.ActiveContractDataManager;

import java.io.IOException;

public class AcceptContractPacket extends Packet {

	private String contractUUID;

	public AcceptContractPacket() {}

	public AcceptContractPacket(String contractUUID) {
		this.contractUUID = contractUUID;
	}

	@Override
	public void readPacketData(PacketReadBuffer buf) throws IOException {
		contractUUID = buf.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buf) throws IOException {
		buf.writeString(contractUUID);
	}

	@Override
	public void processPacketOnClient() {
		// No client-side processing needed
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		ContractData contract = ContractDataManager.getInstance(true).getFromUUID(contractUUID, true);
		if(contract == null) {
			Contracts.getInstance().logWarning("AcceptContractPacket: contract not found: " + contractUUID);
			return;
		}
		boolean accepted = ActiveContractDataManager.getInstance(true).acceptContract(contract, playerState);
		if(accepted) {
			Contracts.getInstance().logInfo("Player " + playerState.getName() + " accepted contract: " + contract.getName());
			playerState.sendServerMessage(new ServerMessage(
					new String[]{"Contract accepted: " + contract.getName()},
					ServerMessage.MESSAGE_TYPE_INFO));
		} else {
			playerState.sendServerMessage(new ServerMessage(
					new String[]{"Cannot accept this contract."},
					ServerMessage.MESSAGE_TYPE_ERROR));
		}
	}
}
