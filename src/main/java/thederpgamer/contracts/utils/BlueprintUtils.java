package thederpgamer.contracts.utils;

import api.common.GameServer;
import com.bulletphysics.linearmath.Transform;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.catalog.CatalogManager;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.controller.*;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprint.SegmentControllerSpawnCallbackDirect;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.blueprintnw.BlueprintType;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.settings.StateParameterNotFoundException;
import org.schema.schine.resource.FileExt;
import org.w3c.tidy.Out;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.manager.ConfigManager;

import javax.vecmath.Vector3f;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class BlueprintUtils {

	private static final HashMap<BlueprintEntry, CatalogPermission> blueprintMap = new HashMap<>();
	private static long lastBPUpdateCheck;

	private static void readBPs() {
		if(blueprintMap.isEmpty() || lastBPUpdateCheck == 0 || System.currentTimeMillis() - lastBPUpdateCheck > ConfigManager.getMainConfig().getLong("blueprint-update-interval")) {
			blueprintMap.clear();
			try {
				List<BlueprintEntry> allBlueprints = BluePrintController.active.readBluePrints();
				List<CatalogPermission> catalogPermissions = getAllCatalogPermissions();
				for(BlueprintEntry blueprint : allBlueprints) {
					for(CatalogPermission catalogPermission : catalogPermissions) {
						if(Objects.equals(catalogPermission.getUid(), blueprint.getName())) {
							blueprintMap.put(blueprint, catalogPermission);
							break;
						}
					}
				}
			} catch(Exception exception) {
				Contracts.getInstance().logException("An error occurred while getting blueprints", exception);
			}
			lastBPUpdateCheck = System.currentTimeMillis();
		}
	}

	public static CatalogPermission getCatalogPermission(BlueprintEntry blueprint) {
		try {
			readBPs();
			for(BlueprintEntry entry : blueprintMap.keySet()) {
				if(Objects.equals(entry.getName(), blueprint.getName())) return blueprintMap.get(entry);
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while getting catalog permission", exception);
		}
		return null;
	}

	public static ArrayList<BlueprintEntry> getPirateBPs() {
		ArrayList<BlueprintEntry> pirateBPs = new ArrayList<>();
		try {
			readBPs();
			for(BlueprintEntry blueprint : blueprintMap.keySet()) {
				if(blueprintMap.get(blueprint).enemyUsable() && blueprint.getType() == BlueprintType.SHIP) pirateBPs.add(blueprint);
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while getting pirate blueprints", exception);
		}
		return pirateBPs;
	}

	public static HashMap<BlueprintEntry, Float> getPirateSpawnWeights() {
		HashMap<BlueprintEntry, Float> pirateSpawnWeights = new HashMap<>();
		try {
			ArrayList<BlueprintEntry> pirateBPs = getPirateBPs();
			for(BlueprintEntry blueprint : pirateBPs) {
				double mass = blueprint.getMass();
				float spawnWeight = (float) (1.0 - (mass / 1000000.0));
				pirateSpawnWeights.put(blueprint, spawnWeight);
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while getting pirate spawn weights", exception);
		}
		return pirateSpawnWeights;
	}

	public static ArrayList<CatalogPermission> getAllCatalogPermissions() {
		ArrayList<CatalogPermission> catalogPermissions = new ArrayList<>();
		try {
			catalogPermissions.addAll(getCatalogManager().getCatalog());
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while getting catalog permissions", exception);
		}
		return catalogPermissions;
	}

	private static CatalogManager getCatalogManager() {
		return GameServer.getServerState().getCatalogManager();
	}

	public static SegmentController spawnAsMob(String bpName, String spawnName, Vector3i sector, int factionId) {
		try {
			BlueprintEntry entry = getBlueprint(bpName);
			if(GameServer.getServerState().getSegmentControllersByName().containsKey(spawnName)) return null;
			if(entry != null) {
				Transform transform = new Transform();
				transform.setIdentity();
				transform.set(getRandomTransform());
				Vector3f forward = GlUtil.getForwardVector(new Vector3f(), transform);
				Vector3f size = entry.getBb().calculateHalfSize(new Vector3f());
				size.scale(0.5f);
				forward.scaleAdd(1.15f, size);
				transform.origin.set(forward);
				try {
					SegmentControllerOutline<?> outline = BluePrintController.active.loadBluePrint(GameServerState.instance, bpName, spawnName, transform, -1, factionId, sector, "Server", PlayerState.buffer, null, true, new ChildStats(false));
					return outline.spawn(sector, false, new ChildStats(false), new SegmentControllerSpawnCallbackDirect(GameServer.getServerState(), sector) {
						@Override
						public void onNoDocker() {
						}
					});
				} catch(EntityNotFountException | IOException | EntityAlreadyExistsException | StateParameterNotFoundException exception) {
					Contracts.getInstance().logException("An error occurred while spawning mob", exception);
				}
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while spawning mob", exception);
		}
		return null;
	}

	public static Transform getRandomTransform() {
		//Gen Random Position
		int sectorSize = (Integer) ServerConfig.SECTOR_SIZE.getCurrentState();
		int x = (int) (Math.random() * sectorSize) * 30;
		int y = (int) (Math.random() * sectorSize) * 30;
		int z = (int) (Math.random() * sectorSize) * 30;
		Vector3f posInSector = new Vector3f(x, y, z);
		int xMult = Math.random() > 0.5 ? 1 : -1;
		int yMult = Math.random() > 0.5 ? 1 : -1;
		int zMult = Math.random() > 0.5 ? 1 : -1;
		posInSector.x *= xMult;
		posInSector.y *= yMult;
		posInSector.z *= zMult;

		//Gen random rotation
		float yaw = (float) (Math.random() * 360);
		float pitch = (float) (Math.random() * 360);
		float roll = (float) (Math.random() * 360);

		//Create Transform
		Transform transform = new Transform();
		transform.setIdentity();
		transform.origin.set(posInSector);
		//Set the transforms matrix
		transform.basis.rotX(pitch);
		transform.basis.rotY(yaw);
		transform.basis.rotZ(roll);
		return transform; //Hope and pray it doesn't collide with anything
	}

	public static BlueprintEntry getBlueprint(String bpName) {
		try {
			readBPs();
			for(BlueprintEntry blueprint : blueprintMap.keySet()) {
				if(Objects.equals(blueprint.getName(), bpName)) return blueprint;
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while getting blueprint", exception);
		}
		return null;
	}

	public static ArrayList<BlueprintEntry> getEscortShips() {
		ArrayList<BlueprintEntry> escortShips = new ArrayList<>();
		InputStream inputStream = Contracts.getInstance().getJarResource("blueprints/trading_guild.zip");
		if(inputStream != null) {
			File tempFile = new FileExt("temp.zip");
			try {
				org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, tempFile);
				File tempFolder = new FileExt(DataUtils.getWorldDataPath() + "/temp");
				FileUtils.unzip(tempFile, tempFolder);
				for(File file : tempFolder.listFiles()) {
					List<BlueprintEntry> entry = BluePrintController.active.importFile(file, null);
					for(BlueprintEntry blueprint : entry) {
						if(blueprint.getType() == BlueprintType.SHIP && blueprint.getName().startsWith("B")) escortShips.add(blueprint);
					}
				}
				tempFolder.delete();
			} catch(Exception exception) {
				exception.printStackTrace();
				Contracts.getInstance().logException("An error occurred while getting escort ships", exception);
			}
		}
		return escortShips;
	}

	public static ArrayList<BlueprintEntry> getCargoShips() {
		ArrayList<BlueprintEntry> cargoShips = new ArrayList<>();
		InputStream inputStream = Contracts.getInstance().getJarResource("blueprints/trading_guild.zip");
		if(inputStream != null) {
			File tempFile = new File("temp.zip");
			try {
				org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, tempFile);
				File tempFolder = new FileExt(DataUtils.getWorldDataPath() + "/temp");
				FileUtils.unzip(tempFile, tempFolder);
				for(File file : tempFolder.listFiles()) {
					List<BlueprintEntry> entry = BluePrintController.active.importFile(file, null);
					for(BlueprintEntry blueprint : entry) {
						if(blueprint.getType() == BlueprintType.SHIP && blueprint.getName().startsWith("C")) cargoShips.add(blueprint);
					}
				}
				tempFolder.delete();
			} catch(Exception exception) {
				exception.printStackTrace();
				Contracts.getInstance().logException("An error occurred while getting cargo ships", exception);
			}
		}
		return cargoShips;
	}

	public static BlueprintEntry getRandomCargoShip() {
		ArrayList<BlueprintEntry> cargoShips = getCargoShips();
		if(cargoShips != null && !cargoShips.isEmpty()) {
			int index = (int) (Math.random() * cargoShips.size());
			return cargoShips.get(index);
		}
		return null;
	}

	public static BlueprintEntry getRandomEscortShip() {
		ArrayList<BlueprintEntry> escortShips = getEscortShips();
		if(escortShips != null && !escortShips.isEmpty()) {
			int index = (int) (Math.random() * escortShips.size());
			return escortShips.get(index);
		}
		return null;
	}
}
