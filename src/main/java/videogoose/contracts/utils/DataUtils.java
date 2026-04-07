package videogoose.contracts.utils;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import videogoose.contracts.Contracts;

public class DataUtils {
	private static final ModSkeleton instance = Contracts.getInstance().getSkeleton();

	public static String getWorldDataPath() {
		return getResourcesPath() + "/data/" + GameCommon.getUniqueContextId();
	}

	public static String getResourcesPath() {
		return instance.getResourcesFolder().getPath().replace('\\', '/');
	}
}