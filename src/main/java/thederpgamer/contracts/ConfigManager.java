package thederpgamer.contracts;

import api.mod.config.FileConfiguration;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ConfigManager {

    private static final String[] defaultMainConfig = {
            "contract-timer-max: 3600000",
            "client-max-active-contracts: 5",
            "auto-generate-contracts: true",
            "max-auto-generate-contracts: 15",
            "auto-generate-contract-check-timer: 300",
            "contract-timeout-timer: 600000" //If nobody claims a contract after this time, it will be removed
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
