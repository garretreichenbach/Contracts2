package thederpgamer.contracts.utils;

import api.common.GameCommon;
import api.common.GameServer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.common.data.world.Universe;
import thederpgamer.contracts.Contracts;

import java.io.IOException;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class SectorUtils {

	public static Vector3i getRandomSector(int range) {
		Vector3i sector = new Vector3i();
		while(true) {
			int xMultiplier = Universe.getRandom().nextInt(2) == 0 ? 1 : -1;
			int x = Universe.getRandom().nextInt(range) - (range / 2) * xMultiplier;
			int yMultiplier = Universe.getRandom().nextInt(2) == 0 ? 1 : -1;
			int y = Universe.getRandom().nextInt(range) - (range / 2) * yMultiplier;
			int zMultiplier = Universe.getRandom().nextInt(2) == 0 ? 1 : -1;
			int z = Universe.getRandom().nextInt(range) - (range / 2) * zMultiplier;
			Vector3i temp = new Vector3i(x, y, z);
			if(/*!containsStations(temp) && */!tooCloseToStar(temp)) {
				sector.set(temp);
				break;
			}
		}
		return sector;
	}

	public static boolean containsStations(Vector3i sector) {
		try {
			Sector s = GameServer.getUniverse().getSector(sector);
			return s.getStationType() != SpaceStation.SpaceStationType.EMPTY;
		} catch(IOException exception) {
			Contracts.getInstance().logException("An error occurred while checking if sector contains stations", exception);
		}
		return false;
	}

	public static boolean tooCloseToStar(Vector3i sector) {
		try {
			Sector s = GameServer.getUniverse().getSector(sector);
			StellarSystem system = s._getSystem();
			float heatRange = GameCommon.getGameState().sunMinIntensityDamageRange;
			if(system.isHeatDamage(sector, s._getSunIntensity(), s._getDistanceToSun(), heatRange) 	|| system.getDistanceIntensity(s._getSunIntensity(), s._getDistanceToSun()) < heatRange * 2) return true;
		} catch(IOException exception) {
			Contracts.getInstance().logException("An error occurred while checking if sector is too close to star", exception);
		}
		return false;
	}
}
