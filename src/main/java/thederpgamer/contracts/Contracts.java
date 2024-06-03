package thederpgamer.contracts;

import api.mod.StarLoader;
import api.mod.StarMod;
import thederpgamer.contracts.data.commands.CompleteContractsCommand;
import thederpgamer.contracts.data.commands.ListContractsCommand;
import thederpgamer.contracts.data.commands.PurgeContractsCommand;
import thederpgamer.contracts.data.commands.RandomContractsCommand;

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
        GUIManager.initialize(this);
        registerCommands();
    }

    private void registerCommands() {
        StarLoader.registerCommand(new RandomContractsCommand());
        StarLoader.registerCommand(new PurgeContractsCommand());
        StarLoader.registerCommand(new CompleteContractsCommand());
        StarLoader.registerCommand(new ListContractsCommand());
    }
}