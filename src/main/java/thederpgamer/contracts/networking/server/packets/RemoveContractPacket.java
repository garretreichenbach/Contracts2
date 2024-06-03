package thederpgamer.contracts.networking.server.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class RemoveContractPacket extends Packet {

    private String contractId;

    public RemoveContractPacket() {}

    public RemoveContractPacket(String contractId) {
        this.contractId = contractId;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        contractId = packetReadBuffer.readString();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(contractId);
    }

    @Override
    public void processPacketOnClient() {
        ClientDataManager.removeClientData(contractId);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
