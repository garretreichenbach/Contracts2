package thederpgamer.contracts.networking.client.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class CompleteContractPacket extends Packet {

    private String contractUID;

    public CompleteContractPacket() {}

    public CompleteContractPacket(String contractUID) {
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
        Contract contract = ServerDataManager.getContractFromId(contractUID);
        if(contract != null) ServerDataManager.completeContract(ServerDataManager.getPlayerData(playerState), contract);
        else throw new NullPointerException("Contract with UID \"" + contractUID + "\" does not exist");
    }
}
