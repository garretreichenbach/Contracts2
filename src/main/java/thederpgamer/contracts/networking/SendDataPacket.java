package thederpgamer.contracts.networking;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.SerializableData;
import thederpgamer.contracts.data.contract.ContractData;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.data.player.PlayerDataManager;

import java.io.IOException;

public class SendDataPacket extends Packet {

	private SerializableData.DataType dataType;
	private SerializableData data;
	private int type;

	public SendDataPacket() {}

	public SendDataPacket(SerializableData data, int type) {
		this.data = data;
		this.type = type;
		dataType = data.getDataType();
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) {
		try {
			type = packetReadBuffer.readInt();
			dataType = SerializableData.DataType.valueOf(packetReadBuffer.readString());
			if(dataType == SerializableData.DataType.CONTRACT_DATA) {
				data = ContractData.readContract(packetReadBuffer);
			} else {
				data = dataType.getDataClass().getDeclaredConstructor(PacketReadBuffer.class).newInstance(packetReadBuffer);
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while reading data packet", exception);
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(type);
		packetWriteBuffer.writeString(dataType.name());
		data.serializeNetwork(packetWriteBuffer);
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
			default:
				throw new IllegalStateException("Unexpected value: " + dataType);
		}
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		switch(dataType) {
			case PLAYER_DATA:
				PlayerDataManager.getInstance(playerState.isOnServer()).handlePacket(data, type, playerState.isOnServer());
				break;
			case CONTRACT_DATA:
				ContractDataManager.getInstance(playerState.isOnServer()).handlePacket(data, type, playerState.isOnServer());
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + dataType);
		}
	}
}
