package thederpgamer.contracts;

import api.common.GameCommon;
import org.schema.game.server.data.simulation.npc.NPCFaction;

import java.util.HashMap;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class NPCContractManager {

	private static final HashMap<Integer, NPCFaction> npcFactions = new HashMap<>();

	public static void initialize() {
		for(String i : ConfigManager.getMainConfig().getList("npc-factions")) {
			int id = Integer.parseInt(i);
			npcFactions.put(id, (NPCFaction) GameCommon.getGameState().getFactionManager().getFaction(id));
		}
		//Todo: Do something with the npcFactions once StarLoader has the new events
	}
}
