package thederpgamer.contracts.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.manager.ConfigManager;
import thederpgamer.contracts.manager.TestManager;

import javax.annotation.Nullable;

public class RunTestCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "run_test";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getDescription() {
		return "Runs a unit test for the Contracts mod. Requires debug-mode to be enabled in the mod's config.\n" +
				"- /%COMMAND% <test_name>|<*/all>: Runs the specified test or all tests if '*' or 'all' is used.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args.length == 0) {
			return false;
		}
		if(!ConfigManager.getMainConfig().getBoolean("debug-mode")) {
			PlayerUtils.sendMessage(sender, "Debug mode must be enabled in the mod's config to run unit tests.");
			return false;
		}
		String testName = args[0].toLowerCase();
		String[] testArgs = new String[args.length - 1];
		System.arraycopy(args, 1, testArgs, 0, testArgs.length);
		if(testName.equals("*") || testName.equals("all")) {
			testName = "all";
		}
		TestManager.runTests(sender, testName, testArgs);
		return true;
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return Contracts.getInstance();
	}
}
