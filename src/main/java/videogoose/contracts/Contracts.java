package videogoose.contracts;

import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import videogoose.contracts.commands.*;
import videogoose.contracts.data.contract.ContractDataManager;
import videogoose.contracts.manager.ConfigManager;
import videogoose.contracts.manager.EventManager;
import videogoose.contracts.manager.GUIManager;
import videogoose.contracts.manager.TestManager;
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