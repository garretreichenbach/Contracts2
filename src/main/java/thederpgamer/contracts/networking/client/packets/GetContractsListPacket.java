package thederpgamer.contracts.networking.client.packets;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.contract.ClientContractData;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.server.ServerActionType;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.io.IOException;
import java.util.ArrayList;

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
        ArrayList<ClientContractData> contractDataList = new ArrayList<>();
        for(Contract contract : ServerDataManager.getAllContracts()) {
            contractDataList.add(new ClientContractData(contract));
        }
        ServerActionType.SEND_CONTRACTS_LIST.send(playerState, contractDataList);
    }
}
