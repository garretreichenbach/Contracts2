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
public class SetCanCompletePacket extends Packet {

    private String contractUID;

    public SetCanCompletePacket() {}

    public SetCanCompletePacket(String contractUID) {
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
        ClientDataManager.setCanComplete(contractUID);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
