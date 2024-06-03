package thederpgamer.contracts.networking.server.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.ConfigManager;
import thederpgamer.contracts.data.contract.ClientContractData;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class SendContractPacket extends Packet {

    private String UID;
    private String name;
    private int contractor;
    private long reward;
    private Contract.ContractType contractType;

    public SendContractPacket() {}

    public SendContractPacket(String UID, String name, int contractor, long reward, Contract.ContractType contractType) {
        this.UID = UID;
        this.name = name;
        this.contractor = contractor;
        this.reward = reward;
        this.contractType = contractType;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        UID = packetReadBuffer.readString();
        name = packetReadBuffer.readString();
        contractor = packetReadBuffer.readInt();
        reward = packetReadBuffer.readLong();
        contractType = Contract.ContractType.values()[packetReadBuffer.readInt()];
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(UID);
        packetWriteBuffer.writeString(name);
        packetWriteBuffer.writeInt(contractor);
        packetWriteBuffer.writeLong(reward);
        packetWriteBuffer.writeInt(contractType.ordinal());
    }

    @Override
    public void processPacketOnClient() {
        ClientDataManager.addContract(new ClientContractData(UID, name, contractor, reward, ConfigManager.getMainConfig().getLong("contract-timer-max"), contractType));
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
