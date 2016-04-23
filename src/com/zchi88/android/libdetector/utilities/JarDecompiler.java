package com.zchi88.android.libdetector.utilities;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;

import org.benf.cfr.reader.Main;

/**
 * 
 * This jar decompiler is simply using cfr_0_115 to decompile java JAR's. Modify
 * this class if you wish to use a different decompiler.
 *
 */
public class JarDecompiler {

	/**
	 * 
	 * Decompile a single Jar file.
	 *
	 * @param jarPath
	 *            - the path to the jarFile
	 * @param outputPath
	 *            - the directory where the decompiled contents will be placed
	 */
	public static void decompileJar(Path jarPath, Path outputPath, Boolean showOutput) {
		if (showOutput) {
			System.out.println(jarPath.getFileName() + " has not been decompiled. Decompiling now...");
		}
		
		final PrintStream showStream = System.out;

		final PrintStream hideStream = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
			}
		});
		final String[] arg = { jarPath.toString(), "--outputpath", outputPath.toString(), "--silent", "true",  "--clobber", "true" };

		// The CFR decompiler prints superfluous decompiling information to
		// the console which we don't care about. This hides it.
		System.setOut(hideStream);
		Main.main(arg);
		System.setOut(showStream);
		hideStream.close();
		
		if (showOutput) {
			System.out.println(jarPath.getFileName() + " has been successfully decompiled.\n");
		}
	}

	/**
	 * Checks to see if all JARs in the directory have been decompiled. If not,
	 * decompile them.
	 * 
	 * 
	 */
	public static void decompileAllJars(Path directory, Boolean showOutput) {
		if (showOutput) {
			System.out.format("Checking if all JAR's for %s have been decompiled...\n", directory);
		}
		
		Boolean isDecompiled = true;

		File[] jarFiles = directory.toFile().listFiles();

		ArrayList<File> jarList = new ArrayList<File>();

		// Generate a list of all jar files in a directory
		if (jarFiles.length > 0) {
			for (File jar : jarFiles) {
				String nameOfJar = jar.toString();
				if (nameOfJar.endsWith(".jar")) {
					jarList.add(jar);
				}
			}
		}

		int jarCount = 0;
		for (File jar : jarList) {
			jarCount++;
			// Get the name of the folder where a JAR's decompiled files
			// would go
			String decompiledFolder;
			if (jarList.size() > 1) {
				decompiledFolder = jar.getParent() + "//source_code" + "(" + jarCount + ")";
			} else {
				decompiledFolder = jar.getParent() + "//source_code";
			}
			

			// Construct the file path for the decompiled files folder.
			File decompiledFolderPath = new File(decompiledFolder);

			// Check if that folder exists
			if (!decompiledFolderPath.exists()) {
				// Decompile the JAR and create this directory if it
				// does not exist 
				isDecompiled = false;
				JarDecompiler.decompileJar(jar.toPath(), decompiledFolderPath.toPath(), showOutput);
			}
		}

		if (isDecompiled && showOutput) {
			System.out.println("Done.");
		}
	}
}
