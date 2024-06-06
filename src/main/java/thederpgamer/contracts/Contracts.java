package thederpgamer.contracts;

import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import thederpgamer.contracts.data.commands.CompleteContractsCommand;
import thederpgamer.contracts.data.commands.ListContractsCommand;
import thederpgamer.contracts.data.commands.PurgeContractsCommand;
import thederpgamer.contracts.data.commands.RandomContractsCommand;
import thederpgamer.contracts.networking.client.packets.*;
import thederpgamer.contracts.networking.server.ServerDataManager;
import thederpgamer.contracts.networking.server.packets.RemoveContractPacket;
import thederpgamer.contracts.networking.server.packets.SendContractPacket;
import thederpgamer.contracts.networking.server.packets.SendContractsListPacket;
import thederpgamer.contracts.networking.server.packets.UpdateClientTimerPacket;

public class Contracts extends StarMod {

    private static Contracts instance;
    public static Contracts getInstance() {
        return instance;
    }
    public Contracts() { }
    public static void main(String[] args) { }

    @Override
    public void onEnable() {
        instance = this;
        ConfigManager.initialize(this);
        EventManager.initialize(this);
        GUIManager.initialize();
        registerCommands();
        registerPackets();
    }

    @Override
    public void onServerCreated(ServerInitializeEvent event) {
        if(ConfigManager.getMainConfig().getBoolean("auto-generate-contracts")) {
            NPCContractManager.initialize();
            (new StarRunnable() {
                @Override
                public void run() {
                    if(ServerDataManager.getAllContracts().size() < ConfigManager.getMainConfig().getInt("max-auto-generate-contracts")) {
                        ServerDataManager.generateRandomContract();
                    }
                }
            }).runTimer(this, ConfigManager.getMainConfig().getLong("auto-generate-contract-check-timer"));
        }
    }

    private void registerCommands() {
        StarLoader.registerCommand(new RandomContractsCommand());
        StarLoader.registerCommand(new PurgeContractsCommand());
        StarLoader.registerCommand(new CompleteContractsCommand());
        StarLoader.registerCommand(new ListContractsCommand());
    }

    private void registerPackets() {
        PacketUtil.registerPacket(RemoveContractPacket.class);
        PacketUtil.registerPacket(SendContractPacket.class);
        PacketUtil.registerPacket(SendContractsListPacket.class);
        PacketUtil.registerPacket(UpdateClientTimerPacket.class);
        PacketUtil.registerPacket(CancelContractClaimPacket.class);
        PacketUtil.registerPacket(CancelContractPacket.class);
        PacketUtil.registerPacket(CompleteContractPacket.class);
        PacketUtil.registerPacket(GetContractPacket.class);
        PacketUtil.registerPacket(GetContractsListPacket.class);
    }
}