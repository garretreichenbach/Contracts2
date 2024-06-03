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
public class ClaimContractPacket extends Packet {

    private String uid;

    public ClaimContractPacket() {}

    public ClaimContractPacket(String uid) {
        this.uid = uid;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        uid = packetReadBuffer.readString();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(uid);
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        ServerDataManager.claimContract(playerState, uid);
    }
}
