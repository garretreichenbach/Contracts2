package thederpgamer.contracts.networking.server;

import api.common.GameServer;
import api.network.Packet;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.networking.server.packets.*;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public enum ServerActionType {
    UPDATE_CONTRACT_TIMER(UpdateClientTimerPacket.class, String.class, Long.class),
    REMOVE_CONTRACT(RemoveContractPacket.class, String.class),
    SEND_CONTRACT(SendContractPacket.class, String.class, String.class, Integer.class, Long.class),
    SEND_CONTRACTS_LIST(SendContractsListPacket.class),
    SET_CAN_COMPLETE(SetCanCompletePacket.class, String.class);

    private final Class<? extends Packet> packetClass;
    private final Class<?>[] argClasses;

    ServerActionType(Class<? extends Packet> packetClass, Class<?>... argClasses) {
        this.packetClass = packetClass;
        this.argClasses = argClasses;
        PacketUtil.registerPacket(packetClass);
    }

    public void send(PlayerState target, Object... args) {
        try {
            PacketUtil.sendPacket(target, packetClass.getConstructor(argClasses).newInstance(args));
        } catch(Exception exception) {
            Contracts.getInstance().logException("An error occurred while executing client action: " + name(), exception);
        }
    }

    public void sendAll(Object... args) {
        for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) send(playerState, args);
    }
}