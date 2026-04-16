package videogoose.contracts.manager;

import api.utils.simpleconfig.SimpleConfigBool;
import api.utils.simpleconfig.SimpleConfigContainer;
import api.utils.simpleconfig.SimpleConfigDouble;
import api.utils.simpleconfig.SimpleConfigInt;
import api.utils.simpleconfig.SimpleConfigString;
import videogoose.contracts.Contracts;

public final class ConfigManager {

    private static SimpleConfigContainer config;

    private static SimpleConfigBool debugMode;
    private static SimpleConfigBool autoGenerateContracts;
    private static SimpleConfigInt maxAutoGenerateContracts;
    private static SimpleConfigInt autoGenerateContractCheckTimer;
    private static SimpleConfigInt contractTimerMax;
    private static SimpleConfigInt contractTimeoutTimer;
    private static SimpleConfigInt clientMaxActiveContracts;
    private static SimpleConfigInt blueprintUpdateInterval;
    private static SimpleConfigInt maxBountyMobCount;
    private static SimpleConfigInt maxBountyMobCombinedMass;
    private static SimpleConfigString rewardType;
    private static SimpleConfigInt rewardItemId;
    private static SimpleConfigDouble rewardBaseMultiplier;
    private static SimpleConfigBool autoBountyEnabled;
    private static SimpleConfigInt autoBountyKillThreshold;
    private static SimpleConfigInt autoBountyReward;
    private static SimpleConfigInt autoBountyDecayTimer;

    private ConfigManager() {}

    public static void initialize(Contracts instance) {
        config = new SimpleConfigContainer(instance, "config", false);

        debugMode                      = new SimpleConfigBool(config, "debug_mode",                         false,   "If true, enables debug logging.");
        autoGenerateContracts          = new SimpleConfigBool(config, "auto_generate_contracts",             true,    "If true, the server will periodically generate random contracts.");
        maxAutoGenerateContracts       = new SimpleConfigInt(config,  "max_auto_generate_contracts",         10,      "Maximum number of auto-generated contracts allowed at once.");
        autoGenerateContractCheckTimer = new SimpleConfigInt(config,  "auto_generate_contract_check_timer",  3000,    "Interval in milliseconds between auto-generation checks.");
        contractTimerMax               = new SimpleConfigInt(config,  "contract_timer_max",                  3600000, "Maximum contract duration in milliseconds.");
        contractTimeoutTimer           = new SimpleConfigInt(config,  "contract_timeout_timer",              900000,  "Time in milliseconds before an unclaimed contract expires.");
        clientMaxActiveContracts       = new SimpleConfigInt(config,  "client_max_active_contracts",         5,       "Maximum number of contracts a single player can hold at once.");
        blueprintUpdateInterval        = new SimpleConfigInt(config,  "blueprint_update_interval",           300000,  "Interval in milliseconds between blueprint cache refreshes.");
        maxBountyMobCount              = new SimpleConfigInt(config,  "max_bounty_mob_count",                12,      "Maximum number of mobs in a single bounty contract.");
        maxBountyMobCombinedMass       = new SimpleConfigInt(config,  "max_bounty_mob_combined_mass",        350000,  "Maximum combined mass of mobs in a single bounty contract.");
        rewardType                     = new SimpleConfigString(config, "reward_type",                        "CREDITS", "Reward type for contracts. CREDITS = pay credits, ITEM = pay items.");
        rewardItemId                   = new SimpleConfigInt(config,  "reward_item_id",                      -1,      "Block/item ID to use when reward_type is ITEM (e.g. gold bar ID).");
        rewardBaseMultiplier           = new SimpleConfigDouble(config, "reward_base_multiplier",             1.0,     "Base multiplier applied to all contract rewards before difficulty scaling.");
        autoBountyEnabled              = new SimpleConfigBool(config, "auto_bounty_enabled",                 true,    "If true, NPC factions will automatically place bounties on aggressive players.");
        autoBountyKillThreshold        = new SimpleConfigInt(config,  "auto_bounty_kill_threshold",           3,      "Number of NPC faction kills before that faction places an auto-bounty.");
        autoBountyReward               = new SimpleConfigInt(config,  "auto_bounty_reward",                   5000000,  "Base credit reward for auto-generated NPC bounties.");
        autoBountyDecayTimer           = new SimpleConfigInt(config,  "auto_bounty_decay_timer",              10800000, "Time in milliseconds before a player's aggression count decays by one.");

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
        return clampInt(intOrDefault(autoGenerateContractCheckTimer, 3000), 1000, 60000);
    }

    public static long getContractTimerMax() {
        return clampInt(intOrDefault(contractTimerMax, 3600000), 60000, 86400000);
    }

    public static long getContractTimeoutTimer() {
        return clampInt(intOrDefault(contractTimeoutTimer, 900000), 60000, 86400000);
    }

    public static int getClientMaxActiveContracts() {
        return clampInt(intOrDefault(clientMaxActiveContracts, 5), 1, 50);
    }

    public static long getBlueprintUpdateInterval() {
        return clampInt(intOrDefault(blueprintUpdateInterval, 300000), 10000, 3600000);
    }

    public static int getMaxBountyMobCount() {
        return clampInt(intOrDefault(maxBountyMobCount, 12), 1, 50);
    }

    public static int getMaxBountyMobCombinedMass() {
        return clampInt(intOrDefault(maxBountyMobCombinedMass, 350000), 1000, 10000000);
    }

    public static boolean isAutoBountyEnabled() {
        return boolOrDefault(autoBountyEnabled, true);
    }

    public static int getAutoBountyKillThreshold() {
        return clampInt(intOrDefault(autoBountyKillThreshold, 5), 1, 100);
    }

    public static long getAutoBountyReward() {
        return intOrDefault(autoBountyReward, 50000);
    }

    public static long getAutoBountyDecayTimer() {
        return clampInt(intOrDefault(autoBountyDecayTimer, 600000), 10000, 86400000);
    }

    public static String getRewardType() {
        return stringOrDefault(rewardType, "CREDITS").toUpperCase();
    }

    public static boolean isItemReward() {
        return "ITEM".equals(getRewardType());
    }

    public static short getRewardItemId() {
        return (short) intOrDefault(rewardItemId, -1);
    }

    public static double getRewardBaseMultiplier() {
        double val = doubleOrDefault(rewardBaseMultiplier, 1.0);
        return Math.max(0.01, val);
    }

    // --- helpers ---

    private static boolean boolOrDefault(SimpleConfigBool entry, boolean def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static int intOrDefault(SimpleConfigInt entry, int def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static String stringOrDefault(SimpleConfigString entry, String def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static double doubleOrDefault(SimpleConfigDouble entry, double def) {
        return (entry == null || entry.getValue() == null) ? def : entry.getValue();
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
