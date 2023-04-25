package thederpgamer.modtemplate;

import api.config.BlockConfig;
import api.mod.StarMod;
import org.apache.commons.io.IOUtils;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.modtemplate.element.ElementManager;
import thederpgamer.modtemplate.element.items.ExampleItem;
import thederpgamer.modtemplate.manager.ConfigManager;
import thederpgamer.modtemplate.manager.EventManager;
import thederpgamer.modtemplate.manager.PacketManager;
import thederpgamer.modtemplate.manager.ResourceManager;
import thederpgamer.modtemplate.utils.DataUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModTemplate extends StarMod {

	//Instance
	private static ModTemplate instance;
	public static ModTemplate getInstance() {
		return instance;
	}
	public static void main(String[] args) {}
	public ModTemplate() {}

	//Logging
	private static Logger log;
	public static void logInfo(String message) {
		log.info("[" + instance.getName() + "]" + message);
	}

	public static void logWarning(String message, @Nullable Exception exception) {
		log.warning("[" + instance.getName() + "]" + message);
		if(exception != null) exception.printStackTrace();
	}

	public static void logError(String message, @Nullable Exception exception) {
		log.severe("[" + instance.getName() + "]" + message);
		if(exception != null) exception.printStackTrace();
	}

	//Other
	private final String[] overwriteClasses = { //Use this to overwrite specific vanilla classes

	};

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		initLogger();
		EventManager.initialize(this);
		PacketManager.initialize();
	}

	@Override
	public byte[] onClassTransform(String className, byte[] byteCode) {
		for(String name : overwriteClasses) if(className.endsWith(name)) return overwriteClass(className, byteCode);
		return super.onClassTransform(className, byteCode);
	}

	@Override
	public void onResourceLoad(ResourceLoader loader) {
		ResourceManager.loadResources();
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementManager.addItem(new ExampleItem());
		ElementManager.initialize();
	}

	private byte[] overwriteClass(String className, byte[] byteCode) {
		byte[] bytes = null;
		try {
			ZipInputStream file = new ZipInputStream(new FileInputStream(this.getSkeleton().getJarFile()));
			while(true) {
				ZipEntry nextEntry = file.getNextEntry();
				if(nextEntry == null) break;
				if(nextEntry.getName().endsWith(className + ".class")) bytes = IOUtils.toByteArray(file);
			}
			file.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		if(bytes != null) return bytes;
		else return byteCode;
	}

	private void initLogger() {
		String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
		File logsFolder = new File(logFolderPath);
		if(!logsFolder.exists()) logsFolder.mkdirs();
		else {
			if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
				File[] logFiles = new File[logsFolder.listFiles().length];
				int j = logFiles.length - 1;
				for(int i = 0; i < logFiles.length && j >= 0; i++) {
					try {
						if(!logFiles[i].getName().endsWith(".lck")) logFiles[j] = logFiles[i];
						else logFiles[i].delete();
						j--;
					} catch(Exception ignored) { }
				}

				//Trim null entries
				int nullCount = 0;
				for(File value : logFiles) {
					if(value == null) nullCount ++;
				}

				File[] trimmedLogFiles = new File[logFiles.length - nullCount];
				int l = 0;
				for(File file : logFiles) {
					if(file != null) {
						trimmedLogFiles[l] = file;
						l++;
					}
				}

				for(File logFile : trimmedLogFiles) {
					if(logFile == null) continue;
					String fileName = logFile.getName().replace(".txt", "");
					int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3)) + 1;
					String newName = logFolderPath + "/log" + logNumber + ".txt";
					if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) logFile.renameTo(new File(newName));
					else logFile.delete();
				}
			}
		}
		try {
			File newLogFile = new File(logFolderPath + "/log0.txt");
			if(newLogFile.exists()) newLogFile.delete();
			newLogFile.createNewFile();
			log = Logger.getLogger(newLogFile.getPath());
			FileHandler handler = new FileHandler(newLogFile.getPath());
			log.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}
}
