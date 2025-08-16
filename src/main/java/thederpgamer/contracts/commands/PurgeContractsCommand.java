package thederpgamer.contracts.commands;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.apache.commons.lang3.math.NumberUtils;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.ContractData;
import thederpgamer.contracts.data.contract.ContractDataManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class PurgeContractsCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "contracts_purge";
	}

	@Override
	public String[] getAliases() {
		return new String[]{"contract_purge", "purge_contracts", "purge_contract"};
	}

	@Override
	public String getDescription() {
		return "Purges multiple contracts from the list matching a type filter if specified.\n" + "- /%COMMAND% <amount|all/*> [filter...] : Purges the specified amount of contracts from the list. If a type filter is specified, only thederpgamer.contracts of the matching type will be purged.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args.length >= 1) {
			String[] purgeFilter = (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length - 1) : new String[0];
			if(args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("*")) {
				purgeContracts(sender, ContractDataManager.getInstance(sender.isOnServer()).getCache(sender.isOnServer()).size(), purgeFilter);
			} else if(NumberUtils.isNumber(args[0])) {
				purgeContracts(sender, Integer.parseInt(args[0]), purgeFilter);
			} else {
				return false;
			}
		} else {
			return false;
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

	private void purgeContracts(PlayerState sender, int amount, String... filter) {
		int i = 0;
		ContractDataManager manager = ContractDataManager.getInstance(sender.isOnServer());
		ArrayList<ContractData> toRemove = new ArrayList<>();
		if(filter != null && filter.length > 0) {
			ArrayList<ContractData.ContractType> types = new ArrayList<>();
			for(String s : filter) {
				ContractData.ContractType type = ContractData.ContractType.fromString(s);
				if(type != null) types.add(type);
				else {
					PlayerUtils.sendMessage(sender, s + " is not a valid contract type.");
					return;
				}
			}
			for(ContractData contract : manager.getCache(sender.isOnServer())) {
				if(types.contains(contract.getContractType())) {
					toRemove.add(contract);
					if(toRemove.size() >= amount) {
						break; // Stop if we have enough contracts to remove
					}
				}
			}
		} else {
			for(ContractData contract : manager.getCache(sender.isOnServer())) {
				toRemove.add(contract);
				if(toRemove.size() >= amount) {
					break; // Stop if we have enough contracts to remove
				}
			}
		}

		for(ContractData contract : toRemove) {
			manager.removeData(contract, sender.isOnServer());
		}
		PersistentObjectUtil.save(Contracts.getInstance().getSkeleton());
		PlayerUtils.sendMessage(sender, "Removed " + (i + 1) + " contracts from database.");
	}
}
