package thederpgamer.contracts.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.json.JSONArray;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.data.player.PlayerDataManager;

import javax.annotation.Nullable;
import java.util.Arrays;

public class ListContractsCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "contracts_list";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"contract_list", "list_contracts", "list_contract"};
	}

	@Override
	public String getDescription() {
		return "\n" + "- /%COMMAND% [player] [filter...] : Lists the active thederpgamer.contracts for the sender (or a player, if specified) as well as their ids, rewards, and progress. Can be filtered to only show specific types.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args == null || args.length == 0) {
			listContracts(sender, PlayerDataManager.getInstance(sender.isOnServer()).getFromName(sender.getName(), sender.isOnServer()));
		} else {
			PlayerData target = PlayerDataManager.getInstance(sender.isOnServer()).getFromName(args[0], sender.isOnServer());
			if(target == null) {
				PlayerUtils.sendMessage(sender, "Player " + args[0] + " doesn't exist!");
			} else {
				if(args.length > 1) {
					listContracts(sender, target, Arrays.copyOfRange(args, 1, args.length - 1));
				} else {
					listContracts(sender, target);
				}
			}
		}
		return true;
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return Contracts.getInstance();
	}

	private void listContracts(PlayerState sender, PlayerData target, String... filter) {
		if(target == null) {
			PlayerUtils.sendMessage(sender, "Player data not found for " + (sender != null ? sender.getName() : "unknown player") + ".");
			return;
		}
		PlayerUtils.sendMessage(sender, "Listing contracts for player: " + target.getName() + " (Faction ID: " + target.getFactionID() + ")");
		if(target.getContracts() == null || target.getContracts().isEmpty()) {
			PlayerUtils.sendMessage(sender, "No active contracts found for player " + target.getName() + ".");
			return;
		}
		ContractDataManager contractDataManager = ContractDataManager.getInstance(sender.isOnServer());
		PlayerUtils.sendMessage(sender, "Active contracts for " + target.getName() + ":");
		JSONArray builder = new JSONArray();
		if(filter != null && filter.length > 0) {
			for(String s : filter) {
				if(!target.getContracts().contains(s)) {
					PlayerUtils.sendMessage(sender, s + " is not a valid contract type.");
					return;
				}
			}
			for(String contract : target.getContracts()) {
				if(Arrays.asList(filter).contains(contract)) {
					builder.put(contractDataManager.getFromUUID(contract, sender.isOnServer()));
				}
			}
		} else {
			for(String contract : target.getContracts()) {
				builder.put(contractDataManager.getFromUUID(contract, sender.isOnServer()));
			}
		}
		if(builder.length() > 0) {
			PlayerUtils.sendMessage(sender, builder.toString());
		} else {
			PlayerUtils.sendMessage(sender, "No contracts match the specified filter.");
		}
	}
}
