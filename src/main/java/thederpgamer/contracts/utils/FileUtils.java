package thederpgamer.contracts.utils;

import thederpgamer.contracts.Contracts;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class FileUtils {

	public static void unzip(File zipFile, File destination) {
		try {
			ZipFile zip = new ZipFile(zipFile);
			Enumeration<ZipEntry> files = (Enumeration<ZipEntry>) zip.entries();
			while(files.hasMoreElements()) {
				ZipEntry entry = files.nextElement();
				File file = new File(destination, entry.getName());
				if(entry.isDirectory()) file.mkdirs();
				else file.createNewFile();
				org.apache.commons.io.FileUtils.copyInputStreamToFile(zip.getInputStream(entry), file);
			}
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while unzipping file", exception);
		}
	}
}
