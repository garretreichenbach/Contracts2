package videogoose.contracts;

import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import videogoose.contracts.commands.*;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.manager.ConfigManager;
import videogoose.contracts.manager.EscortManager;
import videogoose.contracts.manager.EventManager;
import videogoose.contracts.manager.GUIManager;
import videogoose.contracts.networking.AcceptContractPacket;
import videogoose.contracts.networking.SendDataPacket;
import videogoose.contracts.networking.SyncRequestPacket;

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
        if(ConfigManager.isDebugMode()) {
            logInfo("Debug mode enabled");
        }
    }

    @Override
    public void onServerCreated(ServerInitializeEvent event) {
        if(ConfigManager.isAutoGenerateContracts()) {
            new StarRunnable() {
                @Override
                public void run() {
                    ContractDataManager mgr = ContractDataManager.getInstance(true);
                    if(mgr.getCache(true).size() < ConfigManager.getMaxAutoGenerateContracts()) {
                        getInstance().logInfo("Generating random contract data...");
                        mgr.generateRandomContract();
                        getInstance().logInfo("Random contract data generated.");
                    } else {
                        getInstance().logInfo("Maximum number of auto-generated contracts reached. No new contracts will be generated.");
                    }
                }
            }.runTimer(this, ConfigManager.getAutoGenerateContractCheckTimer());
        }
        // Escort mission update timer
        new StarRunnable() {
            @Override
            public void run() {
                EscortManager.getInstance().update();
            }
        }.runTimer(this, ConfigManager.getEscortUpdateInterval());
    }

    private void registerCommands() {
        StarLoader.registerCommand(new RandomContractsCommand());
        StarLoader.registerCommand(new PurgeContractsCommand());
        StarLoader.registerCommand(new CompleteContractsCommand());
        StarLoader.registerCommand(new ListContractsCommand());
    }

    private void registerPackets() {
        PacketUtil.registerPacket(AcceptContractPacket.class);
        PacketUtil.registerPacket(SendDataPacket.class);
        PacketUtil.registerPacket(SyncRequestPacket.class);
    }
}