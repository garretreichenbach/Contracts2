package thederpgamer.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;
import java.util.List;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class BountyContract extends Contract implements ActiveContractRunnable {

    private boolean killedTarget;
    private String target;
    private BountyTargetType targetType = BountyTargetType.PLAYER;
    private BountyTargetMobSpawnGroup targetGroup;

    public BountyContract(int contractorID, String name, long reward, String target) {
        super(contractorID, name, reward);
        this.target = target;
        targetType = BountyTargetType.PLAYER;
    }

    public BountyContract(int contractorID, String name, long reward, BountyTargetMobSpawnGroup targetGroup) {
        super(contractorID, name, reward);
        this.targetGroup = targetGroup;
        target = targetGroup.getName();
        targetType = BountyTargetType.NPC;
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
        if(targetType == BountyTargetType.PLAYER) {
            return ((player.isAdmin() && player.isUseCreativeMode()) || (!target.equals(player.getName()) && killedTarget)) && claimants.containsKey(player.getName());
        } else return (player.isAdmin() && player.isUseCreativeMode()) || killedTarget;
    }

    @Override
    public ContractType getContractType() {
        return ContractType.BOUNTY;
    }

    @Override
    public boolean canStartRunner(PlayerState player) {
        return true;
//        return player.getCurrentSector().equals(targetGroup.getSector());
    }

    @Override
    public List<SegmentController> startRunner(PlayerState player) {
        return targetGroup.spawnGroup();
    }

    @Override
    public boolean updateRunner(PlayerState player, List<?> spawnedMobs) {
        if(targetGroup.isGroupDead((List<SegmentController>) spawnedMobs)) {
            killedTarget = true;
            return false;
        }
        return true;
    }

    @Override
    public void onCompletion(PlayerState player) {
        assert player.isOnServer();
        player.setCredits(player.getCredits() + reward);
    }

    @Override
    public void readFromBuffer(PacketReadBuffer readBuffer) throws IOException {
        super.readFromBuffer(readBuffer);
        target = readBuffer.readString();
        killedTarget = readBuffer.readBoolean();
        targetType = BountyTargetType.values()[readBuffer.readInt()];
        if(targetType == BountyTargetType.NPC) targetGroup = new BountyTargetMobSpawnGroup(readBuffer);
    }

    @Override
    public void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException {
        super.writeToBuffer(writeBuffer);
        writeBuffer.writeString(target);
        writeBuffer.writeBoolean(killedTarget);
        writeBuffer.writeInt(targetType.ordinal());
        if(targetType == BountyTargetType.NPC) targetGroup.writeToBuffer(writeBuffer);
    }

    @Override
    public void fromJSON(JSONObject json) {
        super.fromJSON(json);
        target = json.getString("target");
        killedTarget = json.getBoolean("killedTarget");
        if(json.has("targetType")) targetType = BountyTargetType.valueOf(json.getString("targetType"));
        if(json.has("targetGroup")) targetGroup = new BountyTargetMobSpawnGroup(json.getJSONObject("targetGroup"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("target", target);
        json.put("killedTarget", killedTarget);
        json.put("targetType", targetType.toString());
        if(targetType == BountyTargetType.NPC) json.put("targetGroup", targetGroup.toJSON());
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

    public BountyTargetType getTargetType() {
        return targetType;
    }

    public BountyTargetMobSpawnGroup getTargetGroup() {
        return targetGroup;
    }

    public enum BountyTargetType {
        PLAYER,
        NPC
    }
}
