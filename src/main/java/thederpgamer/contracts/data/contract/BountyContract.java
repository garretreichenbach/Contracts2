package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class BountyContract extends Contract {

    private boolean killedTarget;
    private String target;

    public BountyContract(int contractorID, String name, int reward, String target) {
        super(contractorID, name, reward);
        this.target = target;
    }

    public BountyContract(PacketReadBuffer packetReadBuffer) throws IOException {
        super(packetReadBuffer);
    }

    public BountyContract(JSONObject json) {
        super(json);
        fromJSON(json);
    }

    @Override
    public boolean canComplete(PlayerState player) {
        return ((player.isAdmin() && player.isUseCreativeMode()) || (!target.equals(player.getName()) && killedTarget)) && claimants.containsKey(player.getName());
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
        writeBuffer.writeString(target);
        writeBuffer.writeBoolean(killedTarget);
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        target = json.getString("target");
        killedTarget = json.getBoolean("killedTarget");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("target", target);
        json.put("killedTarget", killedTarget);
        return json;
    }

    public String getTarget() {
        return target;
    }

    public boolean hasKilledTarget() {
        return killedTarget;
    }

    public void setKilledTarget(boolean killedTarget) {
        this.killedTarget = killedTarget;
    }
}
