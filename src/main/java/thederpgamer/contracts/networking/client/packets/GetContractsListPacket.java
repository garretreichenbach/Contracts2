package thederpgamer.contracts.networking.client.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.networking.server.ServerActionType;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class GetContractsListPacket extends Packet {

    public GetContractsListPacket() {}

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        ServerActionType.SEND_CONTRACTS_LIST.send(playerState);
    }
}
