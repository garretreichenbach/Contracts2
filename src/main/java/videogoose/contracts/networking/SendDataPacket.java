package videogoose.contracts.networking;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import videogoose.contracts.Contracts;
import videogoose.contracts.data.SerializableData;
import videogoose.contracts.data.contract.ContractData;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.data.contract.active.ActiveContractData;
import videogoose.contracts.data.contract.active.ActiveContractDataManager;
import videogoose.contracts.data.player.PlayerData;
import videogoose.contracts.data.player.PlayerDataManager;

import java.io.IOException;

public class SendDataPacket extends Packet {

	private SerializableData.DataType dataType;
	private SerializableData data;
	private int type;

	public SendDataPacket() {}

	public SendDataPacket(SerializableData data, int type) {
		if(data == null) throw new IllegalArgumentException("Data cannot be null!");
		this.data = data;
		this.type = type;
		dataType = data.getDataType();
	}

	@Override
	public void readPacketData(PacketReadBuffer buf) throws IOException {
		type = buf.readInt();
		dataType = SerializableData.DataType.valueOf(buf.readString());
		switch(dataType) {
			case PLAYER_DATA:
				data = new PlayerData(buf);
				break;
			case CONTRACT_DATA:
				data = ContractData.readContract(buf);
				break;
			case ACTIVE_CONTRACT_DATA:
				data = new ActiveContractData(buf);
				break;
			default:
				throw new IOException("Unknown DataType: " + dataType);
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer buf) throws IOException {
		buf.writeInt(type);
		buf.writeString(dataType.name());
		data.serializeNetwork(buf);
	}

	@Override
	public void processPacketOnClient() {
		switch(dataType) {
			case PLAYER_DATA:
				PlayerDataManager.getInstance(false).handlePacket(data, type, false);
				break;
			case CONTRACT_DATA:
				ContractDataManager.getInstance(false).handlePacket(data, type, false);
				break;
			case ACTIVE_CONTRACT_DATA:
				ActiveContractDataManager.getInstance(false).handlePacket(data, type, false);
				break;
			default:
				Contracts.getInstance().logWarning("SendDataPacket: unhandled DataType on client: " + dataType);
		}
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		boolean server = playerState.isOnServer();
		switch(dataType) {
			case PLAYER_DATA:
				PlayerDataManager.getInstance(server).handlePacket(data, type, server);
				break;
			case CONTRACT_DATA:
				ContractDataManager.getInstance(server).handlePacket(data, type, server);
				break;
			case ACTIVE_CONTRACT_DATA:
				ActiveContractDataManager.getInstance(server).handlePacket(data, type, server);
				break;
			default:
				Contracts.getInstance().logWarning("SendDataPacket: unhandled DataType on server: " + dataType);
		}
	}
}
