package thederpgamer.contracts.networking.server.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class SendContractPacket extends Packet {

    private Contract contract;

    public SendContractPacket() {}

    public SendContractPacket(Contract contract) {
        this.contract = contract;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        contract = Contract.readContract(packetReadBuffer);
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        contract.writeToBuffer(packetWriteBuffer);
    }

    @Override
    public void processPacketOnClient() {
        ClientDataManager.addContract(contract);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
