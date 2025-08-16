package thederpgamer.contracts;

import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import thederpgamer.contracts.commands.*;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.manager.EventManager;
import thederpgamer.contracts.manager.GUIManager;
import thederpgamer.contracts.manager.TestManager;
import thederpgamer.contracts.networking.SendDataPacket;
import thederpgamer.contracts.networking.SyncRequestPacket;

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
        if(ConfigManager.getMainConfig().getBoolean("debug-mode")) {
            TestManager.initialize();
            logInfo("Debug mode enabled");
        }
    }

    @Override
    public void onServerCreated(ServerInitializeEvent event) {
        if(ConfigManager.getMainConfig().getBoolean("auto-generate-contracts")) {
            (new StarRunnable() {
                @Override
                public void run() {
                    ContractDataManager instance = ContractDataManager.getInstance(true);
                    if(instance.getCache(true).size() < ConfigManager.getMainConfig().getInt("max-auto-generate-contracts")) {
                        getInstance().logInfo("Generating random contract data...");
                        instance.generateRandomContract();
                        getInstance().logInfo("Random contract data generated.");
                    } else {
                        getInstance().logInfo("Maximum number of auto-generated contracts reached. No new contracts will be generated.");
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
        StarLoader.registerCommand(new RunTestCommand());
    }

    private void registerPackets() {
        PacketUtil.registerPacket(SendDataPacket.class);
        PacketUtil.registerPacket(SyncRequestPacket.class);
    }
}