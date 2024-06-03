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
public class CreateContractPacket extends Packet {

    private Contract contract;

    public CreateContractPacket() {}

    public CreateContractPacket(Contract contract) {
        this.contract = contract;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        contract = Contract.readContract(packetReadBuffer);
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        contract.writeContract(packetWriteBuffer);
    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        ServerDataManager.createContract(playerState, contract);
    }
}
