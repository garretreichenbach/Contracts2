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
public class UpdateClientTimerPacket extends Packet {

    private String contractUID;
    private long timeRemaining;

    public UpdateClientTimerPacket() {}

    public UpdateClientTimerPacket(String contractUID, Long timeRemaining) {
        this.contractUID = contractUID;
        this.timeRemaining = timeRemaining;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        contractUID = packetReadBuffer.readString();
        timeRemaining = packetReadBuffer.readLong();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(contractUID);
        packetWriteBuffer.writeLong(timeRemaining);
    }

    @Override
    public void processPacketOnClient() {
        ClientDataManager.updateClientTimer(contractUID, timeRemaining);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
