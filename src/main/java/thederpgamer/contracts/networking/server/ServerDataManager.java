package thederpgamer.contracts.networking.server;

import api.common.GameCommon;
import api.common.GameServer;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import api.utils.StarRunnable;
import api.utils.game.inventory.ItemStack;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.PlayerNotFountException;
import thederpgamer.contracts.ConfigManager;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.BountyContract;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.data.contract.ItemsContract;
import thederpgamer.contracts.data.player.PlayerData;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

/**
 * Manages server data and contains functions for saving and loading it from file.
 *
 * @author TheDerpGamer
 */
public class ServerDataManager {

    private static final ModSkeleton instance = Contracts.getInstance().getSkeleton();

    /**
     * Locates any existing playerData matching the updated playerData's name and replaces them with the updated version.
     *
     * @param playerData The updated PlayerData.
     */
    public static void updatePlayerData(PlayerData playerData) {
        ArrayList<Object> objectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
        ArrayList<PlayerData> toRemove = new ArrayList<>();
        for(Object playerDataObject : objectList) {
            PlayerData pData = (PlayerData) playerDataObject;
            if(pData.name.equals(playerData.name)) toRemove.add(pData);
        }

        for(PlayerData pData : toRemove) PersistentObjectUtil.removeObject(instance, pData);
        PersistentObjectUtil.addObject(instance, playerData);
    }

    /**
     * Gets the PlayerData for a player based off their name.
     *
     * @param playerName The player's name.
     * @return The player's PlayerData. Returns null if no matching data is found with the specified name.
     */
    public static PlayerData getPlayerData(String playerName) {
        ArrayList<Object> objectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
        for(Object playerDataObject : objectList) {
            PlayerData playerData = (PlayerData) playerDataObject;
            if(playerData.name.equals(playerName)) return playerData;
        }
        return null;
    }

    /**
     * Gets the PlayerData for a player based off their PlayerState.
     *
     * @param playerState The player's state.
     * @return The player's PlayerData. Creates a new entry if no existing PlayerData is found matching the PlayerState.
     */
    public static PlayerData getPlayerData(PlayerState playerState) {
        PlayerData playerData;
        if((playerData = getPlayerData(playerState.getName())) != null) {
            return playerData;
        } else {
            return createNewPlayerData(playerState);
        }
    }

    private static PlayerData createNewPlayerData(PlayerState playerState) {
        PlayerData playerData = new PlayerData(playerState);
        PersistentObjectUtil.addObject(instance, playerData);
        return playerData;
    }

    /**
     * Creates an ArrayList containing the ids of a specified faction's allies and returns it.
     *
     * @param factionId The faction's id.
     * @return An ArrayList containing the ids of the faction's allies.
     */
    public static ArrayList<Integer> getFactionAllies(int factionId) {
        ArrayList<Integer> factionAllies = new ArrayList<>();
        Faction faction;
        if((faction = GameCommon.getGameState().getFactionManager().getFaction(factionId)) != null) {
            for(Faction ally : faction.getFriends()) factionAllies.add(ally.getIdFaction());
        }
        return factionAllies;
    }

    /**
     * Removes a contract with the specified UID from the list of contracts.
     *
     * @param UID The UID of the contract to remove.
     */
    public static void removeContract(String UID) {
        ArrayList<Contract> contracts = getAllContracts();
        for(Contract contract : contracts) {
            if(Objects.equals(contract.getUID(), UID)) PersistentObjectUtil.removeObject(instance, contract);
        }
        PersistentObjectUtil.save(instance);
        ServerActionType.REMOVE_CONTRACT.sendAll(UID);
    }

    /**
     * Adds or updates a contract in the list of contracts.
     *
     * @param contract The contract to add or update.
     */
    public static void addOrUpdateContract(Contract contract) {
        removeContract(contract.getUID());
        PersistentObjectUtil.addObject(instance, contract);
        PersistentObjectUtil.save(instance);
        ServerActionType.SEND_CONTRACT.sendAll(contract);
    }

    /**
     * Retrieves all contracts from the server.
     *
     * @return An ArrayList of Contract objects representing all contracts.
     */
    public static ArrayList<Contract> getAllContracts() {
        ArrayList<ItemsContract> itemsContracts = getItemsContracts();
        ArrayList<BountyContract> bountyContracts = getBountyContracts();
        ArrayList<Contract> contracts = new ArrayList<>();
        contracts.addAll(itemsContracts);
        contracts.addAll(bountyContracts);
        return contracts;
    }

    public static ArrayList<ItemsContract> getItemsContracts() {
        return PersistentObjectUtil.getCopyOfObjects(instance, ItemsContract.class);
    }

    public static ArrayList<BountyContract> getBountyContracts() {
        return PersistentObjectUtil.getCopyOfObjects(instance, BountyContract.class);
    }

    /**
     * Creates an ArrayList containing all Contracts a specified player has claimed and returns it.
     *
     * @param playerData The player's data.
     * @return An ArrayList containing the player's claimed thederpgamer.contracts.
     */
    public static ArrayList<Contract> getPlayerContracts(PlayerData playerData) {
        ArrayList<Contract> playerContracts = new ArrayList<>();
        for(Contract contract : getAllContracts()) {
            if(playerData.contracts.contains(contract)) playerContracts.add(contract);
        }
        return playerContracts;
    }

    /**
     * Gets a contract from the list of contracts based off its UID.
     *
     * @param contractId The UID of the contract.
     * @return The contract with the specified UID. Returns null if no matching contract is found.
     */
    public static Contract getContractFromId(String contractId) {
        for(Contract contract : getAllContracts()) {
            if(Objects.equals(contract.getUID(), contractId)) return contract;
        }
        return null;
    }

    /**
     * Starts a timer in which the specified player must complete the contract before it reaches 0.
     *
     * @param contract The contract being started.
     * @param player   The player's data.
     */
    public static void startContractTimer(final Contract contract, final PlayerData player) {
        new StarRunnable() {
            @Override
            public void run() {
                if(contract.getClaimants().containsKey(player)) {
                    if(contract.getClaimants().get(player) >= ConfigManager.getMainConfig().getInt("contract-timer-max")) {
                        try {
                            timeoutContract(contract, player);
                        } catch(PlayerNotFountException e) {
                            e.printStackTrace();
                        }
                        cancel();
                    } else {
                        if(contract.canComplete(getPlayerState(player))) ServerActionType.SET_CAN_COMPLETE.send(getPlayerState(player), contract.getUID());
                        else {
                            contract.getClaimants().put(player, contract.getClaimants().get(player) + 30);
                            ServerActionType.UPDATE_CONTRACT_TIMER.send(getPlayerState(player), contract.getUID(), contract.getClaimants().get(player));
                        }
                    }
                }
            }
        }.runTimer(Contracts.getInstance(), 30);
    }

    public static PlayerState getPlayerState(PlayerData player) {
        return GameServer.getServerState().getPlayerStatesByName().get(player.name);
    }

    /**
     * Completes a contract and rewards the player who completed it.
     *
     * @param playerData The player's data.
     * @param contract   The contract being completed.
     */
    public static void completeContract(PlayerData playerData, Contract contract) {
        playerData.contracts.remove(contract);
        updatePlayerData(playerData);
        addOrUpdateContract(contract);
        contract.onCompletion(getPlayerState(playerData));
        //Todo: Maybe some sort of custom flavor message depending on the contract type and contractor
        playerData.sendMail(contract.getContractor().getName(), "Contract Completion", "You have completed the contract \"" + contract.getName() + "\" and have been rewarded " + contract.getReward() + " credits!");
    }

    /**
     * Removes a player's claim from a contract if they have not completed it before the contract's timer reaches 0.
     *
     * @param contract The contract being removed.
     * @param player   The player who claimed the contract.
     * @throws PlayerNotFountException If the PlayerData is invalid.
     */
    public static void timeoutContract(Contract contract, PlayerData player) throws PlayerNotFountException {
        contract.getClaimants().remove(player);
        assert player != null;
        player.contracts.remove(contract);
        addOrUpdateContract(contract);
        updatePlayerData(player);
        player.sendMail(contract.getContractor().getName(), "Contract Cancellation", contract.getContractor().getName() + " has cancelled your contract because you took too long!");
    }

    /**
     * Generates a random contract and adds it to the list.
     */
    public static void generateRandomContract() {
        Random random = new Random();
        Contract.ContractType contractType = Contract.ContractType.getRandomType();
        ArrayList<Short> possibleIDs = new ArrayList<>();
        String contractName;

        int amountInt = random.nextInt(3000 - 100) + 100;
        int basePrice;
        ItemStack target;
        Contract randomContract = null;

        switch(contractType) {
            case ITEMS:
                for(ElementInformation info : getProductionFilter()) possibleIDs.add(info.getId());
                int productionIndex = random.nextInt(possibleIDs.size() - 1) + 1;
                short productionID = possibleIDs.get(productionIndex);
                contractName = "Produce x" + amountInt + " " + ElementKeyMap.getInfo(productionID).getName();
                target = new ItemStack(productionID, amountInt);
                basePrice = (int) ElementKeyMap.getInfo(productionID).getPrice(true);
                int reward = (int) ((basePrice * amountInt) * 1.3);
                randomContract = new ItemsContract(FactionManager.TRAIDING_GUILD_ID, contractName, reward, target);
                break;
                /*
            case BOUNTY: //Todo: Pick from a list of aggressive players rather than just random ones
                ArrayList<PlayerState> playerStates = new ArrayList<>(GameServer.getServerState().getPlayerStatesByName().values());
                if(playerStates.isEmpty()) return;
                PlayerState targetPlayer = playerStates.get(random.nextInt(playerStates.size()));
                contractName = "Kill " + targetPlayer.getName();
                int bountyAmount = random.nextInt(10000 - 1000) + 1000;
                randomContract = new BountyContract(FactionManager.TRAIDING_GUILD_ID, contractName, bountyAmount, targetPlayer.getName());
                break;
                 */
        }
        if(randomContract != null) addOrUpdateContract(randomContract);
    }

    public static ArrayList<ElementInformation> getResourcesFilter() {
        ArrayList<ElementInformation> filter = new ArrayList<>();
        ArrayList<ElementInformation> elementList = new ArrayList<>();
        ElementKeyMap.getCategoryHirarchy().getChild("Manufacturing").getInfoElementsRecursive(elementList);
        for(ElementInformation info : elementList) {
            if(!info.isDeprecated() && info.isShoppable() && info.isInRecipe() && !info.getName().contains("Paint") && !info.getName().contains("Hardener") && !info.getName().contains("Scrap"))
                filter.add(info);
        }
        return filter;
    }

    public static ArrayList<ElementInformation> getProductionFilter() {
        ArrayList<ElementInformation> filter = new ArrayList<>();
        ArrayList<ElementInformation> elementList = new ArrayList<>();
        ElementKeyMap.getCategoryHirarchy().getChild("General").getInfoElementsRecursive(elementList);
        ElementKeyMap.getCategoryHirarchy().getChild("Ship").getInfoElementsRecursive(elementList);
        ElementKeyMap.getCategoryHirarchy().getChild("SpaceStation").getInfoElementsRecursive(elementList);
        for(ElementInformation info : elementList) {
            if(!info.isDeprecated() && info.isShoppable() && info.isInRecipe()) filter.add(info);
        }
        return filter;
    }

    public static void claimContract(PlayerState playerState, String uid) {
        PlayerData playerData = getPlayerData(playerState);
        Contract contract = getContractFromId(uid);
        if(contract != null) {
            if(!contract.getClaimants().containsKey(playerData)) {
                contract.getClaimants().put(playerData, 0L);
                playerData.contracts.add(contract);
                addOrUpdateContract(contract);
                updatePlayerData(playerData);
                startContractTimer(contract, playerData);
            }
        }
    }

    public static void cancelClaim(PlayerState playerState, String uid) {
        PlayerData playerData = getPlayerData(playerState);
        Contract contract = getContractFromId(uid);
        if(contract != null) {
            if(contract.getClaimants().containsKey(playerData)) {
                contract.getClaimants().remove(playerData);
                playerData.contracts.remove(contract);
                addOrUpdateContract(contract);
                updatePlayerData(playerData);
            }
        }
    }

    /**
     * Cancels a contract and removes it from the list.
     * @param contractUID The UID of the contract being cancelled.
     */
    public static void cancelContract(String contractUID) {
        Contract contract = getContractFromId(contractUID);
        if(contract != null) {
            for(PlayerData player : contract.getClaimants().keySet()) {
                player.contracts.remove(contract);
                updatePlayerData(player);
            }
            removeContract(contractUID);
        }
    }
}
