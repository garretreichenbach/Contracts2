package thederpgamer.contracts.data.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.networking.server.ServerDataManager;
import thederpgamer.contracts.data.contract.Contract;
import thederpgamer.contracts.data.player.PlayerData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <Description>
 *
 * @author TheDerpGamer
 */
public class ListContractsCommand implements CommandInterface {

    @Override
    public String getCommand() {
        return "contracts_list";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "contract_list",
                "list_contracts",
                "list_contract"
        };
    }

    @Override
    public String getDescription() {
        return "\n" +
               "- /%COMMAND% [player] [filter...] : Lists the active thederpgamer.contracts for the sender (or a player, if specified) as well as their ids, rewards, and progress. Can be filtered to only show specific types.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        if(args == null || args.length == 0) listContracts(sender, ServerDataManager.getPlayerData(sender));
        else {
            PlayerData target = ServerDataManager.getPlayerData(args[0]);
            if(target == null) PlayerUtils.sendMessage(sender, "Player " + args[0] + " doesn't exist!");
            else {
                if(args.length > 1) listContracts(sender, target, Arrays.copyOfRange(args, 1, args.length - 1));
                else listContracts(sender, target);
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
        if(target.contracts != null && target.contracts.size() > 0) {
            ArrayList<String> contractList= new ArrayList<>();
            StringBuilder builder = new StringBuilder();
            builder.append("Active contracts for ").append(sender.getName()).append(":\n");
            if(filter != null && filter.length > 0) {
                ArrayList<Contract.ContractType> types = new ArrayList<>();
                for(String s : filter) {
                    Contract.ContractType type = Contract.ContractType.fromString(s);
                    if(type != null) types.add(type);
                    else {
                        PlayerUtils.sendMessage(sender, s + " is not a valid contract type.");
                        return;
                    }
                }
                for(Contract contract : ServerDataManager.getPlayerContracts(target)) {
                    if(types.contains(contract.getContractType())) {
                        contractList.add(contract.getName().trim() + "[" + contract.getName() + "] - $" + contract.getReward() + " | " + contract.getContractorName().trim());
                    }
                }
            } else {
                for(Contract contract : ServerDataManager.getPlayerContracts(target)) {
                    contractList.add(contract.getName().trim() + "[" + contract.getName() + "] - $" + contract.getReward() + " | " + contract.getContractorName().trim());
                }
            }

            for(int i = 0; i < contractList.size(); i ++) {
                builder.append(contractList.get(i));
                if(i < contractList.size() - 1) builder.append("\n");
            }
            PlayerUtils.sendMessage(sender, builder.toString().trim());
        } else PlayerUtils.sendMessage(sender, "No active contracts found for player " + target.name + ".");
    }
}
