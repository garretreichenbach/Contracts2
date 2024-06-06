package thederpgamer.contracts.networking.server;

import api.common.GameCommon;
import api.common.GameServer;
import api.utils.StarRunnable;
import api.utils.game.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.PlayerNotFountException;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.*;
import thederpgamer.contracts.manager.NPCContractManager;
import thederpgamer.contracts.utils.DataUtils;
import thederpgamer.contracts.data.player.PlayerData;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

/**
 * Manages server data and contains functions for saving and loading it from file.
 *
 * @author TheDerpGamer
 */
public class ServerDataManager {

	private static File getPlayerDataFile() {
		File folder = new File(DataUtils.getWorldDataPath());
		if(!folder.exists()) folder.mkdirs();
		File file = new File(DataUtils.getWorldDataPath() + "/playerData.smdat");
		if(!file.exists()) {
			try {
				file.createNewFile();
				JSONArray playerDataJson = new JSONArray();
				Files.write(file.toPath(), playerDataJson.toString().getBytes(StandardCharsets.UTF_8));
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}
		return file;
	}

	public static HashMap<String, PlayerData> getPlayerDataMap() {
		HashMap<String, PlayerData> playerDataMap = new HashMap<>();
		try {
			JSONArray playerDataJson = new JSONArray(new String(Files.readAllBytes(getPlayerDataFile().toPath()), StandardCharsets.UTF_8));
			for(int i = 0; i < playerDataJson.length(); i++) {
				JSONObject playerDataObject = playerDataJson.getJSONObject(i);
				PlayerData playerData = new PlayerData(playerDataObject);
				playerDataMap.put(playerData.name, playerData);
			}
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		return playerDataMap;
	}

	public static void addOrUpdatePlayerData(PlayerData playerData) {
		removePlayerData(playerData);
		HashMap<String, PlayerData> playerDataMap = getPlayerDataMap();
		if(GameServer.getServerState().getPlayerStatesByName().containsKey(playerData.name)) {
			PlayerState playerState = GameServer.getServerState().getPlayerStatesByName().get(playerData.name);
			playerData.factionID = playerState.getFactionId();
		}
		playerDataMap.put(playerData.name, playerData);
		savePlayerData(playerDataMap);
	}

	public static void removePlayerData(PlayerData playerData) {
		HashMap<String, PlayerData> playerDataMap = getPlayerDataMap();
		if(playerDataMap.containsKey(playerData.name)) {
			playerDataMap.remove(playerData.name);
			savePlayerData(playerDataMap);
		}
	}

	public static PlayerData getPlayerData(String playerName) {
		if(!getPlayerDataMap().containsKey(playerName)) {
			try {
				PlayerData playerData = new PlayerData(GameServer.getServerState().getPlayerFromName(playerName));
				addOrUpdatePlayerData(playerData);
				return playerData;
			} catch(PlayerNotFountException exception) {
				exception.printStackTrace();
			}
		}
		return getPlayerDataMap().get(playerName);
	}

	private static void savePlayerData(HashMap<String, PlayerData> playerDataMap) {
		ArrayList<PlayerData> playerDataList = new ArrayList<>(playerDataMap.values());
		savePlayerData(playerDataList);
	}

	private static void savePlayerData(ArrayList<PlayerData> playerDataList) {
		JSONArray playerDataJson = new JSONArray();
		for(PlayerData playerData : playerDataList) playerDataJson.put(playerData.toJSON());
		try {
			Files.write(getPlayerDataFile().toPath(), playerDataJson.toString().getBytes(StandardCharsets.UTF_8));
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}

	private static File getContractDataFile() {
		File folder = new File(DataUtils.getWorldDataPath());
		if(!folder.exists()) folder.mkdirs();
		File file = new File(DataUtils.getWorldDataPath() + "/contractData.smdat");
		if(!file.exists()) {
			try {
				file.createNewFile();
				JSONArray contractDataJson = new JSONArray();
				Files.write(file.toPath(), contractDataJson.toString().getBytes(StandardCharsets.UTF_8));
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}
		return file;
	}

	public static HashMap<String, Contract> getContractDataMap() {
		HashMap<String, Contract> contractDataMap = new HashMap<>();
		try {
			JSONArray contractDataJson = new JSONArray(new String(Files.readAllBytes(getContractDataFile().toPath()), StandardCharsets.UTF_8));
			for(int i = 0; i < contractDataJson.length(); i++) {
				JSONObject contractDataObject = contractDataJson.getJSONObject(i);
				Contract contract = Contract.readContract(contractDataObject);
				contractDataMap.put(contract.getUID(), contract);
			}
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		return contractDataMap;
	}

	public static void addOrUpdateContract(Contract contract) {
		removeContract(contract);
		HashMap<String, Contract> contractDataMap = getContractDataMap();
		contractDataMap.put(contract.getUID(), contract);
		saveContractData(contractDataMap);
		ServerActionType.SEND_CONTRACT.sendAll(contract);
	}

	public static void removeContract(Contract contract) {
		HashMap<String, Contract> contractDataMap = getContractDataMap();
		contractDataMap.remove(contract.getUID());
		saveContractData(contractDataMap);
		ServerActionType.REMOVE_CONTRACT.sendAll(contract.getUID());
	}

	public static void saveContractData(HashMap<String, Contract> contractDataMap) {
		ArrayList<Contract> contractDataList = new ArrayList<>(contractDataMap.values());
		saveContractData(contractDataList);
	}

	public static void saveContractData(ArrayList<Contract> contractDataList) {
		JSONArray contractDataJson = new JSONArray();
		for(Contract contract : contractDataList) contractDataJson.put(contract.toJSON());
		try {
			Files.write(getContractDataFile().toPath(), contractDataJson.toString().getBytes(StandardCharsets.UTF_8));
		} catch(IOException exception) {
			exception.printStackTrace();
		}
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

	public static ArrayList<Contract> getAllContracts() {
		return new ArrayList<>(getContractDataMap().values());
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
			if(playerData.contracts.contains(contract.getUID())) playerContracts.add(contract);
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
		if(contract instanceof ActiveContractRunnable) NPCContractManager.addToActive(player, contract);
		new StarRunnable() {
			@Override
			public void run() {
				if(contract.getClaimants().containsKey(player.name)) {
					if(contract.getClaimants().get(player.name) >= ConfigManager.getMainConfig().getInt("contract-timer-max")) {
						try {
							timeoutContract(contract, player);
						} catch(PlayerNotFountException e) {
							e.printStackTrace();
						}
						cancel();
					} else {
						contract.getClaimants().put(player.name, contract.getClaimants().get(player.name) + 30);
						ServerActionType.UPDATE_CONTRACT_TIMER.send(getPlayerState(player), contract.getUID(), contract.getClaimants().get(player.name));
						if(contract instanceof ActiveContractRunnable && NPCContractManager.isActiveFor(player, contract)) {
							if(!NPCContractManager.update(player, contract)) cancel();
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
		playerData.contracts.remove(contract.getUID());
		addOrUpdatePlayerData(playerData);
		removeContract(contract);
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
		contract.getClaimants().remove(player.name);
		player.contracts.remove(contract.getUID());
		if(contract instanceof ActiveContractRunnable) NPCContractManager.removeFromActive(player, contract);
		addOrUpdateContract(contract);
		addOrUpdatePlayerData(player);
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
		long basePrice;
		ItemStack target;
		Contract randomContract = null;

		switch(contractType) {
			case ITEMS:
				for(ElementInformation info : getProductionFilter()) possibleIDs.add(info.getId());
				int productionIndex = random.nextInt(possibleIDs.size() - 1) + 1;
				short productionID = possibleIDs.get(productionIndex);
				contractName = "Produce x" + amountInt + " " + ElementKeyMap.getInfo(productionID).getName();
				target = new ItemStack(productionID, amountInt);
				basePrice = ElementKeyMap.getInfo(productionID).getPrice(true);
				long reward = (long) ((basePrice * amountInt) * 1.3f);
				randomContract = new ItemsContract(FactionManager.TRAIDING_GUILD_ID, contractName, reward, target);
				break;
            case BOUNTY:
				//Todo: Add bounties on aggressive players
	            BountyTargetMobSpawnGroup group = BountyTargetMobSpawnGroup.generateRandomSpawnGroup();
				if(group == null) return;
	            contractName = "Defeat the " + group.getName() + " in sector " + group.getSector();
				randomContract = new BountyContract(FactionManager.TRAIDING_GUILD_ID, contractName, (long) (group.calculateReward() * 1.3f), group);
	            break;
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
		PlayerData playerData = getPlayerData(playerState.getName());
		Contract contract = getContractFromId(uid);
		if(contract != null) {
			if(!contract.getClaimants().containsKey(playerData.name)) {
				contract.getClaimants().put(playerData.name, 0L);
				playerData.contracts.add(contract.getUID());
				addOrUpdateContract(contract);
				addOrUpdatePlayerData(playerData);
				startContractTimer(contract, playerData);
			}
		}
	}

	public static void cancelClaim(PlayerState playerState, String uid) {
		PlayerData playerData = getPlayerData(playerState.getName());
		Contract contract = getContractFromId(uid);
		if(contract != null) {
			if(contract.getClaimants().containsKey(playerData)) {
				contract.getClaimants().remove(playerData);
				playerData.contracts.remove(contract.getUID());
				addOrUpdateContract(contract);
				addOrUpdatePlayerData(playerData);
			}
		}
	}

	/**
	 * Cancels a contract and removes it from the list.
	 *
	 * @param contractUID The UID of the contract being cancelled.
	 */
	public static void cancelContract(String contractUID) {
		Contract contract = getContractFromId(contractUID);
		if(contract != null) {
			for(String player : contract.getClaimants().keySet()) {
				PlayerData playerData = getPlayerData(player);
				playerData.contracts.remove(contract.getUID());
				addOrUpdatePlayerData(playerData);
			}
			removeContract(contract);
		}
	}

	public static ArrayList<BountyContract> getBountyContracts() {
		ArrayList<BountyContract> bountyContracts = new ArrayList<>();
		for(Contract contract : getAllContracts()) {
			if(contract instanceof BountyContract) bountyContracts.add((BountyContract) contract);
		}
		return bountyContracts;
	}
}
