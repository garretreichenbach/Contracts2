package thederpgamer.contracts.networking.client.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class CancelContractPacket extends Packet {

    private String contractUID;

    public CancelContractPacket() {}

    public CancelContractPacket(String contractUID) {
        this.contractUID = contractUID;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        contractUID = packetReadBuffer.readString();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(contractUID);
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        ServerDataManager.cancelContract(contractUID);
    }
}
