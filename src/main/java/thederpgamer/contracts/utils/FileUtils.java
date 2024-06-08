package thederpgamer.contracts.utils;

import thederpgamer.contracts.Contracts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class FileUtils {

	public static void unzip(File zipFile, File destination) {
		try {
			ZipFile zip = new ZipFile(zipFile);
			destination.mkdirs();
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File file = new File(destination, entry.getName());
				if(entry.isDirectory()) file.mkdirs();
				else {
					file.getParentFile().mkdirs();
					file.createNewFile();
				}
				if(!entry.isDirectory()) org.apache.commons.io.FileUtils.copyInputStreamToFile(zip.getInputStream(entry), file);
				else file.mkdirs();
			}
			zip.close();
		} catch(Exception exception) {
			Contracts.getInstance().logException("An error occurred while unzipping file", exception);
		}
	}
}
