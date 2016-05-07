package com.zchi88.android.libdetector.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 
 * This jar decompiler is simply using cfr_0_115 to decompile java JAR's. Modify
 * this class if you wish to use a different decompiler.
 *
 */
public class JarExtractor {
	/**
	 * 
	 * Extract the bytecode of a single Jar file.
	 *
	 * @param jarPath
	 *            - the path to the jar file
	 * @throws IOException
	 */
	public static void extractByteCode(Path jarPath, Boolean showOutput) throws IOException {
		if (showOutput) {
			System.out.println("Extracting all .class files from " + jarPath.getFileName() + "...");
		}

		// Name of the folder that will hold the extracted class files
		Path extractToFolder = Paths.get("byteCode");
		Path jarExtractionPath = jarPath.getParent().resolve(extractToFolder);
		JarFile jar = new JarFile(jarPath.toString());
		Enumeration<JarEntry> filesInJar = jar.entries();

		while (filesInJar.hasMoreElements()) {
			java.util.jar.JarEntry file = filesInJar.nextElement();

			String nameOfFile = file.getName();

			if (nameOfFile.endsWith(".class")) {
				File outputFile = jarExtractionPath.resolve(nameOfFile).toFile();
				File outputDirectory = outputFile.getParentFile();

				if (!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}

				if (!outputFile.exists()) {
					InputStream is = jar.getInputStream(file);
					FileOutputStream os = new FileOutputStream(outputFile);
					while (is.available() > 0) {
						os.write(is.read());
					}
					os.close();
					is.close();
				}
			}
		}
		jar.close();
	}

	/**
	 * Checks to see if all JARs in a given library have been extracted. If not,
	 * extract them.
	 * 
	 * @throws IOException
	 * 
	 * 
	 */
	public static void extractAllJars(Path pathToJars, Boolean showOutput) throws IOException {
		if (showOutput) {
			System.out.format("Checking if bytecode for all JAR's at %s has been extracted...\n", pathToJars);
		}

		File[] libraryVersions = pathToJars.toFile().listFiles();

		if (libraryVersions.length > 0) {
			for (File libFile : libraryVersions) {
				String nameOfLib = libFile.toString();
				if (nameOfLib.endsWith(".jar")) {
					JarExtractor.extractByteCode(libFile.toPath(), showOutput);
				}
			}
		}
		if (showOutput) {
			System.out.println("Done.");
		}
	}
}
