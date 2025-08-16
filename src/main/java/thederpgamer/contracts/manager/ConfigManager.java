package thederpgamer.contracts.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.contracts.Contracts;

public class ConfigManager {

    private static final String[] defaultMainConfig = {
            "debug-mode: false",
            "contract-timer-max: 3600000",
            "client-max-active-contracts: 5",
            "auto-generate-contracts: true",
            "max-auto-generate-contracts: 10",
            "auto-generate-contract-check-timer: 3000",
            "contract-timeout-timer: 900000",
            "blueprint-update-interval: 300000",
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
