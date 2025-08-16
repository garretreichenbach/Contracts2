package thederpgamer.contracts.manager;

import org.schema.game.common.data.player.PlayerState;
import thederpgamer.tests.Test;
import thederpgamer.tests.data.contract.RandomContractTest;

import java.util.HashMap;

public class TestManager {

	private static final HashMap<String, Test> tests = new HashMap<>();

	public static void initialize() {
		tests.put("random_contract", new RandomContractTest());
	}

	public static void runTests(PlayerState sender, String testName, String... args) {
		if(testName == null || testName.isEmpty()) {
			sender.sendServerMessagePlayerError(new String[] {"No test specified!"});
			return;
		}
		if(testName.equalsIgnoreCase("all")) {
			runAllTests(sender);
		} else {
			for(String test : tests.keySet()) {
				runTest(sender, test);
			}
		}
	}

	private static void runTest(PlayerState sender, String test, String... args) {
		Test t = tests.get(test);
		if(t == null) {
			sender.sendServerMessagePlayerError(new String[] {"Test '" + test + "' does not exist!"});
			return;
		}
		t.setup(sender, args);
		String[] result = t.runTest();
		sender.sendServerMessagePlayerInfo(result);
	}

	private static void runAllTests(PlayerState sender) {
		if(tests.isEmpty()) {
			sender.sendServerMessagePlayerError(new String[] {"No tests available!"});
			return;
		}
		for(String test : tests.keySet()) {
			runTest(sender, test);
		}
		sender.sendServerMessagePlayerInfo(new String[] {"All tests completed!"});
	}
}
