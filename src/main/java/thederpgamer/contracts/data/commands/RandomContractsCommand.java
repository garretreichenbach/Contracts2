package thederpgamer.contracts.data.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.apache.commons.lang3.math.NumberUtils;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.networking.server.ServerDataManager;

import javax.annotation.Nullable;

/**
 * <Description>
 *
 * @author TheDerpGamer
 */
public class RandomContractsCommand implements CommandInterface {

    @Override
    public String getCommand() {
        return "contracts_random";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "contract_random",
                "random_contracts",
                "random_contract"
        };
    }

    @Override
    public String getDescription() {
        return "Creates a randomly generated contract and adds it to the thederpgamer.contracts list. If an amount is specified, generates multiple random thederpgamer.contracts.\n" +
               "- /%COMMAND% [amount] : Generates multiple random thederpgamer.contracts, or just a single one if no amount is specified.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        if(args != null && args.length == 1) {
            if(NumberUtils.isNumber(args[0].trim()) && Integer.parseInt(args[0].trim()) > 0) generateRandomContract(sender, Integer.parseInt(args[0].trim()));
            else return false;
        } else if(args == null || args.length == 0) generateRandomContract(sender, 1);
        else return false;
        return true;
    }

    @Override
    public void serverAction(@Nullable PlayerState sender, String[] args) {

    }

    @Override
    public StarMod getMod() {
        return Contracts.getInstance();
    }

    private void generateRandomContract(PlayerState sender, int amount) {
        int i;
        for(i = 0; i < amount; i ++) ServerDataManager.generateRandomContract();
        PlayerUtils.sendMessage(sender, "Generated " + i + " random contracts.");
    }
}
