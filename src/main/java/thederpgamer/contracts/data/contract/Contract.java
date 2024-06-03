package thederpgamer.contracts.data.contract;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.networking.server.ServerDataManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Contract {

    protected final String name;
    protected final int contractorID;
    protected final int reward;
    protected HashMap<PlayerData, Long> claimants = new HashMap<>();
    protected Object target;
    protected String uid;
    protected int timer;
    protected boolean finished;

    public Contract(int contractorID, String name, int reward, Object target) {
        this.name = name;
        this.contractorID = contractorID;
        this.reward = reward;
        this.target = target;
        uid = UUID.randomUUID().toString();
        uid = uid.substring(0, uid.indexOf('-'));
        claimants = new HashMap<>();
        timer = -1;
        finished = false;
    }

    protected Contract(PacketReadBuffer readBuffer) throws IOException {
        name = readBuffer.readString();
        contractorID = readBuffer.readInt();
        reward = readBuffer.readInt();
        uid = readBuffer.readString();
        timer = readBuffer.readInt();
        finished = readBuffer.readBoolean();
        int claimantsSize = readBuffer.readInt();
        for(int i = 0; i < claimantsSize; i++) {
            PlayerData playerData = new PlayerData(readBuffer);
            long time = readBuffer.readLong();
            claimants.put(playerData, time);
        }
        readFromBuffer(readBuffer);
    }

    public static Contract readContract(PacketReadBuffer packetReadBuffer) throws IOException {
        ContractType type = ContractType.fromString(packetReadBuffer.readString());
        switch(type) {
            case BOUNTY:
                return new BountyContract(packetReadBuffer);
            case ITEMS:
                return new ItemsContract(packetReadBuffer);
            default:
                return null;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public HashMap<PlayerData, Long> getClaimants() {
        return claimants;
    }

    public String getName() {
        return name;
    }

    public Faction getContractor() {
        if(contractorID != 0) {
            return GameCommon.getGameState().getFactionManager().getFaction(contractorID);
        } else {
            ServerDataManager.removeContract(this);
            return null;
        }
    }

    public String getContractorName() {
        return (contractorID != 0) ? getContractor().getName() : "Non-Aligned";
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public int getReward() {
        return reward;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public String getUID() {
        return uid;
    }

    public abstract boolean canComplete(PlayerState player);

    public abstract ContractType getContractType();

    public abstract void onCompletion(PlayerState player);

    public abstract void readFromBuffer(PacketReadBuffer readBuffer) throws IOException;

    public abstract void writeToBuffer(PacketWriteBuffer writeBuffer) throws IOException;

    public void writeContract(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeString(getContractType().displayName);
        packetWriteBuffer.writeString(name);
        packetWriteBuffer.writeInt(contractorID);
        packetWriteBuffer.writeInt(reward);
        packetWriteBuffer.writeString(uid);
        packetWriteBuffer.writeInt(timer);
        packetWriteBuffer.writeBoolean(finished);
        packetWriteBuffer.writeInt(claimants.size());
        for(PlayerData playerData : claimants.keySet()) {
            playerData.writeToBuffer(packetWriteBuffer);
            packetWriteBuffer.writeLong(claimants.get(playerData));
        }
        writeToBuffer(packetWriteBuffer);
    }

    public enum ContractType {
        ALL("All"),
        BOUNTY("Bounty"),
        ITEMS("Items");

        public final String displayName;

        ContractType(String displayName) {
            this.displayName = displayName;
        }

        public static ContractType fromString(String s) {
            for(ContractType type : values()) {
                if(s.trim().equalsIgnoreCase(type.displayName.trim())) return type;
            }
            return null;
        }

        public static ContractType getRandomType() {
            return values()[(int) (Math.random() * (values().length - 1)) + 1];
        }
    }
}
