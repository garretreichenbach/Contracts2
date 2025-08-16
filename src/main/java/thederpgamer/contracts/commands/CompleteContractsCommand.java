package thederpgamer.contracts.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.data.contract.ContractData;
import thederpgamer.contracts.data.contract.ContractDataManager;
import thederpgamer.contracts.data.player.PlayerData;
import thederpgamer.contracts.data.player.PlayerDataManager;

import javax.annotation.Nullable;

/**
 * <Description>
 *
 * @author TheDerpGamer
 */
public class CompleteContractsCommand implements CommandInterface {

    @Override
    public String getCommand() {
        return "contracts_complete";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "contract_complete",
                "complete_contracts",
                "complete_contract"
        };
    }

    @Override
    public String getDescription() {
        return "Sets the specified contract as completed.\n" +
               "- /%COMMAND% <contract id|all/*> [player] : Completes the specified contract based off it's id for the sender, or a specific player if listed.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        if(args.length >= 1) {
            PlayerData target = null;
            if(args.length > 1) {
                if(PlayerDataManager.getInstance(sender.isOnServer()).getFromName(args[1], sender.isOnServer()) != null) {
                    target = PlayerDataManager.getInstance(sender.isOnServer()).getFromName(args[1], sender.isOnServer());
                } else {
                    PlayerUtils.sendMessage(sender, "Player " + args[1] + " doesn't exist!");
                }
            } else {
                target = PlayerDataManager.getInstance(sender.isOnServer()).getFromName(sender.getName(), sender.isOnServer());
            }
            if(args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("*")) {
                completeContracts(sender, target, target.getContracts().toArray(new ContractData[0]));
            } else {
                ContractData contract = ContractDataManager.getInstance(sender.isOnServer()).getFromUUID(args[0].trim(), sender.isOnServer());
                if(contract != null) completeContracts(sender, target, contract);
                else PlayerUtils.sendMessage(sender, "No valid contracts found with id " + args[0].trim());
            }
        } else return false;
        return true;
    }

    @Override
    public void serverAction(@Nullable PlayerState sender, String[] args) {

    }

    @Override
    public StarMod getMod() {
        return Contracts.getInstance();
    }

    private void completeContracts(PlayerState sender, PlayerData target, ContractData... contracts) {
        if(contracts != null && contracts.length >= 1) {
            StringBuilder builder = new StringBuilder();
            builder.append("Completed the following contracts for player ").append(target.getName()).append(":\n");
            for(int i = 0; i < contracts.length; i ++) {
                ContractData contract = contracts[i];
                if(!contract.getClaimants().containsKey(target.getName())) {
                    PlayerUtils.sendMessage(sender, "Player " + target.getName() + " doesn't have any active contracts matching name " + contract.getName() + ".");
                } else {
                    builder.append(contract.getName());
                    if(i < contracts.length - 1) {
                        builder.append(", ");
                    }
                    ContractDataManager.completeContract(target, contract);
                }
            }
            PlayerUtils.sendMessage(sender, builder.toString().trim());
        } else PlayerUtils.sendMessage(sender, "No active contracts found for player" + target.getName() + ".");
    }
}