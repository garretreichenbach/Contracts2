package thederpgamer.contracts.data.contract;

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
public class BountyContract extends Contract {

    private boolean killedTarget;

    public BountyContract(int contractorID, String name, int reward, String target) {
        super(contractorID, name, reward, target);
    }

    public BountyContract(PacketReadBuffer packetReadBuffer) throws IOException {
        super(packetReadBuffer);
    }

    @Override
    public boolean canComplete(PlayerState player) {
        return ((player.isAdmin() && player.isUseCreativeMode()) || (!target.equals(player.getName()) && killedTarget)) && claimants.containsKey(ServerDataManager.getPlayerData(player));
    }

    @Override
    public ContractType getContractType() {
        return ContractType.BOUNTY;
    }

    @Override
    public void onCompletion(PlayerState player) {
        assert player.isOnServer();
        player.setCredits(player.getCredits() + reward);
    }

    @Override
    public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
        target = readBuffer.readString();
        killedTarget = readBuffer.readBoolean();
    }

    @Override
    public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeString((String) target);
        writeBuffer.writeBoolean(killedTarget);
    }

    public String getTarget() {
        return (String) target;
    }

    public boolean hasKilledTarget() {
        return killedTarget;
    }

    public void setKilledTarget(boolean killedTarget) {
        this.killedTarget = killedTarget;
    }
}
