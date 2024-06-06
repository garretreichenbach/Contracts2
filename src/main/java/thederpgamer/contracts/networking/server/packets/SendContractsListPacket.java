package thederpgamer.contracts.networking.server.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.client.ClientDataManager;

import java.io.IOException;
import java.util.ArrayList;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class SendContractsListPacket extends Packet {

    private ArrayList<Contract> contractDataList = new ArrayList<>();

    public SendContractsListPacket() {}

    public SendContractsListPacket(ArrayList<Contract> contractDataList) {
        this.contractDataList = contractDataList;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        int size = packetReadBuffer.readInt();
        for(int i = 0; i < size; i++) contractDataList.add(Contract.readContract(packetReadBuffer));
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeInt(contractDataList.size());
        for(Contract contractData : contractDataList) contractData.writeToBuffer(packetWriteBuffer);
    }

    @Override
    public void processPacketOnClient() {
        for(Contract contractData : contractDataList) ClientDataManager.addContract(contractData);
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}
