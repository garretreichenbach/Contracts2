package thederpgamer.contracts.data.player;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.data.contract.Contract;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerData {

    public String name;
    public ArrayList<Contract> contracts;
    public int factionID;

    public PlayerData(PacketReadBuffer readBuffer) throws IOException {
        name = readBuffer.readString();
        contracts = new ArrayList<>();
        factionID = readBuffer.readInt();
    }

    public PlayerData(PlayerState playerState) {
        name = playerState.getName();
        contracts = new ArrayList<>();
        factionID = playerState.getFactionId();
    }

    public void sendMail(String from, String title, String contents) {
        GameCommon.getPlayerFromName(name).getClientChannel().getPlayerMessageController().serverSend(from, name, title, contents);
    }

    public void writeToBuffer(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(name);
        packetWriteBuffer.writeInt(factionID);
    }
}