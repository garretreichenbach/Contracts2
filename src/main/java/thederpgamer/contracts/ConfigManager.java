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
            "max-contracts-total: 15",
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
