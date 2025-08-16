package thederpgamer.contracts.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.contracts.Contracts;

public class ConfigManager {

    private static final String[] defaultMainConfig = {
            "debug-mode: false",
            "contract-timer-max: 3600000",
            "client-max-active-contracts: 5",
            "auto-generate-contracts: true",
            "max-auto-generate-contracts: 15",
            "auto-generate-contract-check-timer: 3000",
            "contract-timeout-timer: 600000", //If nobody claims a contract after this time, it will be removed
            "blueprint-update-interval: 300000", //Update bp list every 5 minutes
            "max-bounty-mob-count: 12",
            "max-bounty-mob-combined-mass: 350000"
    };
    private static FileConfiguration mainConfig;

    public static void initialize(Contracts instance) {
        mainConfig = instance.getConfig("config");
        mainConfig.saveDefault(defaultMainConfig);
    }

    public static FileConfiguration getMainConfig() {
        return mainConfig;
    }
}
