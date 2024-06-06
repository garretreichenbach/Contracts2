package thederpgamer.contracts.networking.client;

import api.network.Packet;
import api.network.packets.PacketUtil;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.networking.client.packets.*;

/**
 * Enum for different actions the client can take towards the server.
 *
 * @author Garret Reichenbach
 */
public enum ClientActionType {
    GET_CONTRACTS_LIST(GetContractsListPacket.class), //Request an updated list of contracts from the server
    GET_CONTRACT(GetContractPacket.class, String.class), //Request a specific contract from the server by its uid
    CLAIM_CONTRACT(ClaimContractPacket.class, String.class), //Claim a contract from the server and sets it as active for the client
    CANCEL_CLAIM(CancelContractClaimPacket.class, String.class), //Cancel a claim on a contract the client has taken
    COMPLETE_CONTRACT(CompleteContractPacket.class, String.class), //Complete a contract the client has taken
    CANCEL_CONTRACT(CancelContractPacket.class, String.class),
    CREATE_CONTRACT(CreateContractPacket.class, Contract.class); //Cancel a contract created by the client

    private final Class<? extends Packet> packetClass;
    private final Class<?>[] argClasses;

    ClientActionType(Class<? extends Packet> packetClass, Class<?>... argClasses) {
        this.packetClass = packetClass;
        this.argClasses = argClasses;
    }

    public void send(Object... args) {
        try {
            PacketUtil.sendPacketToServer(packetClass.getConstructor(argClasses).newInstance(args));
        } catch(Exception exception) {
            Contracts.getInstance().logException("An error occurred while executing client action: " + name(), exception);
        }
    }
}