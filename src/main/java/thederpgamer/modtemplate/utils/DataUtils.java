package thederpgamer.modtemplate.utils;

import api.common.GameClient;
import api.common.GameCommon;
import thederpgamer.modtemplate.ModTemplate;

public class DataUtils {

	public static String getResourcesPath() {
		return ModTemplate.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}

	public static String getWorldDataPath() {
		String universeName = GameCommon.getUniqueContextId();
		if(!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
		else {
			try {
				ModTemplate.logWarning("Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.", null);
			} catch(Exception ignored) { }
			return null;
		}
	}
}
