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
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.manager.EventManager;
import thederpgamer.contracts.manager.GUIManager;
import thederpgamer.contracts.manager.NPCContractManager;
import thederpgamer.contracts.networking.client.ClientActionType;
import thederpgamer.contracts.networking.server.ServerActionType;
import thederpgamer.contracts.networking.server.ServerDataManager;

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
        ServerDataManager.initialize();
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
        for(ServerActionType type : ServerActionType.values()) PacketUtil.registerPacket(type.packetClass);
        for(ClientActionType type : ClientActionType.values()) PacketUtil.registerPacket(type.packetClass);
    }
}