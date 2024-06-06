package thederpgamer.contracts.networking.server;

import api.common.GameServer;
import api.network.Packet;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.server.packets.*;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public enum ServerActionType {
    UPDATE_CONTRACT_TIMER(UpdateClientTimerPacket.class, String.class, Long.class),
    REMOVE_CONTRACT(RemoveContractPacket.class, String.class),
    SEND_CONTRACT(SendContractPacket.class, Contract.class),
    SEND_CONTRACTS_LIST(SendContractsListPacket.class, ArrayList.class);

    public final Class<? extends Packet> packetClass;
    public final Class<?>[] argClasses;

    ServerActionType(Class<? extends Packet> packetClass, Class<?>... argClasses) {
        this.packetClass = packetClass;
        this.argClasses = argClasses;
    }

    public void send(PlayerState target, Object... args) {
        try {
            PacketUtil.sendPacket(target, packetClass.getConstructor(argClasses).newInstance(args));
        } catch(Exception exception) {
            Contracts.getInstance().logException("An error occurred while executing server action: " + name(), exception);
        }
    }

    public void sendAll(Object... args) {
        for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) send(playerState, args);
    }
}
