package thederpgamer.contracts.data.contract;

import api.common.GameClient;
import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ClientContractData {

    private String UID;
    private String name;
    private int contractor;
    private long reward;
    private Contract.ContractType contractType;
    private long timeRemaining;
    private boolean claimed;
    private boolean canComplete;

    public ClientContractData(String UID, String name, int contractor, long reward, long timeRemaining, Contract.ContractType contractType) {
        this.UID = UID;
        this.name = name;
        this.contractor = contractor;
        this.reward = reward;
        this.timeRemaining = timeRemaining;
        this.contractType = contractType;
    }

    public ClientContractData(PacketReadBuffer packetReadBuffer) throws IOException {
        readData(packetReadBuffer);
    }

    public String getUID() {
        return UID;
    }

    public String getName() {
        return name;
    }

    public int getContractor() {
        return contractor;
    }

    public long getReward() {
        return reward;
    }

    public long getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(long timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean b) {
        claimed = b;
    }

    public Contract.ContractType getContractType() {
        return contractType;
    }

    public boolean canClaim() {
        PlayerState player = GameClient.getClientPlayerState();
        if(player.getFactionId() == contractor || claimed) return false;
        else {
            Faction contractorFaction = GameCommon.getGameState().getFactionManager().getFaction(contractor);
            return contractorFaction != null && !contractorFaction.getEnemies().contains(player.getFactionId()) && !contractorFaction.getPersonalEnemies().contains(player.getName());
        }
    }

    public boolean canComplete() {
        return canComplete;
    }

    public void setCanComplete(boolean b) {
        canComplete = b;
    }

    public void writeData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(UID);
        packetWriteBuffer.writeString(name);
        packetWriteBuffer.writeInt(contractor);
        packetWriteBuffer.writeLong(reward);
        packetWriteBuffer.writeLong(timeRemaining);
        packetWriteBuffer.writeBoolean(claimed);
        packetWriteBuffer.writeBoolean(canComplete);
        packetWriteBuffer.writeInt(contractType.ordinal());
    }

    public void readData(PacketReadBuffer packetReadBuffer) throws IOException {
        packetReadBuffer.readString();
        packetReadBuffer.readString();
        packetReadBuffer.readInt();
        packetReadBuffer.readLong();
        packetReadBuffer.readLong();
        packetReadBuffer.readBoolean();
        packetReadBuffer.readBoolean();
        packetReadBuffer.readInt();
    }
}
