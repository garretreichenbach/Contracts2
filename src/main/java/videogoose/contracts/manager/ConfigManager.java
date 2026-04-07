package videogoose.contracts.manager;

import api.utils.simpleconfig.SimpleConfigBool;
import api.utils.simpleconfig.SimpleConfigContainer;
import api.utils.simpleconfig.SimpleConfigInt;
import api.utils.simpleconfig.SimpleConfigLong;
import videogoose.contracts.Contracts;

public final class ConfigManager {

    private static SimpleConfigContainer config;

    private static SimpleConfigBool debugMode;
    private static SimpleConfigBool autoGenerateContracts;
    private static SimpleConfigInt maxAutoGenerateContracts;
    private static SimpleConfigLong autoGenerateContractCheckTimer;
    private static SimpleConfigLong contractTimerMax;
    private static SimpleConfigLong contractTimeoutTimer;
    private static SimpleConfigInt clientMaxActiveContracts;
    private static SimpleConfigLong blueprintUpdateInterval;
    private static SimpleConfigInt maxBountyMobCount;
    private static SimpleConfigInt maxBountyMobCombinedMass;

    private ConfigManager() {}

    public static void initialize(Contracts instance) {
        config = new SimpleConfigContainer(instance, "config", false);

        debugMode                    = new SimpleConfigBool(config,  "debug_mode",                        false,   "If true, enables debug logging and test commands.");
        autoGenerateContracts        = new SimpleConfigBool(config,  "auto_generate_contracts",            true,    "If true, the server will periodically generate random contracts.");
        maxAutoGenerateContracts     = new SimpleConfigInt(config,   "max_auto_generate_contracts",        10,      "Maximum number of auto-generated contracts allowed at once.");
        autoGenerateContractCheckTimer = new SimpleConfigLong(config,"auto_generate_contract_check_timer", 3000L,   "Interval in milliseconds between auto-generation checks.");
        contractTimerMax             = new SimpleConfigLong(config,  "contract_timer_max",                 3600000L,"Maximum contract duration in milliseconds.");
        contractTimeoutTimer         = new SimpleConfigLong(config,  "contract_timeout_timer",             900000L, "Time in milliseconds before an unclaimed contract expires.");
        clientMaxActiveContracts     = new SimpleConfigInt(config,   "client_max_active_contracts",        5,       "Maximum number of contracts a single player can hold at once.");
        blueprintUpdateInterval      = new SimpleConfigLong(config,  "blueprint_update_interval",          300000L, "Interval in milliseconds between blueprint cache refreshes.");
        maxBountyMobCount            = new SimpleConfigInt(config,   "max_bounty_mob_count",               12,      "Maximum number of mobs in a single bounty contract.");
        maxBountyMobCombinedMass     = new SimpleConfigInt(config,   "max_bounty_mob_combined_mass",       350000,  "Maximum combined mass of mobs in a single bounty contract.");

        config.readWriteFields();

        if(isDebugMode()) {
            instance.logInfo("Config initialized (mode=" + (config.isServer() ? "server" : "client") + ")");
        }
    }

    public static void reload() {
        if(config != null) config.readFields();
    }

    public static boolean isDebugMode() {
        return boolOrDefault(debugMode, false);
    }

    public static boolean isAutoGenerateContracts() {
        return boolOrDefault(autoGenerateContracts, true);
    }

    public static int getMaxAutoGenerateContracts() {
        return clampInt(intOrDefault(maxAutoGenerateContracts, 10), 1, 100);
    }

    public static long getAutoGenerateContractCheckTimer() {
        return clampLong(longOrDefault(autoGenerateContractCheckTimer, 3000L), 1000L, 60000L);
    }

    public static long getContractTimerMax() {
        return clampLong(longOrDefault(contractTimerMax, 3600000L), 60000L, 86400000L);
    }

    public static long getContractTimeoutTimer() {
        return clampLong(longOrDefault(contractTimeoutTimer, 900000L), 60000L, 86400000L);
    }

    public static int getClientMaxActiveContracts() {
        return clampInt(intOrDefault(clientMaxActiveContracts, 5), 1, 50);
    }

    public static long getBlueprintUpdateInterval() {
        return clampLong(longOrDefault(blueprintUpdateInterval, 300000L), 10000L, 3600000L);
    }

    public static int getMaxBountyMobCount() {
        return clampInt(intOrDefault(maxBountyMobCount, 12), 1, 50);
    }

    public static int getMaxBountyMobCombinedMass() {
        return clampInt(intOrDefault(maxBountyMobCombinedMass, 350000), 1000, 10000000);
    }

    // --- helpers ---

    private static boolean boolOrDefault(SimpleConfigBool entry, boolean def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static int intOrDefault(SimpleConfigInt entry, int def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static long longOrDefault(SimpleConfigLong entry, long def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
