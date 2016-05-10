package com.zchi88.android.libdetector.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

/**
 * Contains methods for processing APKs into source code for analysis Processes
 * APK's by first extracting their byte code, then decompiling that byte code
 * into source code
 * 
 *
 */
public class ApkProcessor {
	// Default to not show console output 
	private static final Boolean DISPLAY_OUTPUT = false;
	
	/**
	 * Processes an APK by first converting its classes.dex file(s) into JAR files, then extracting the bytecode(.class files) out of the JAR.
	 * 
	 * @param apkFile
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void processApk(File apkFile) throws InterruptedException, IOException {
		// Only operate if we are passed a valid APK file
		if (apkFile.getName().endsWith(".apk")) {
			// First extract the APK file to the Extracted_APKs directory
			DexExtractor.extractJars(apkFile, DISPLAY_OUTPUT);
			
			// Then decompile all JAR files that were created from the APK extraction
			Path relExtractionPath = Paths.get("Extracted_APKs");
			Path absExtractionPath = apkFile.toPath().getParent().getParent().resolve(relExtractionPath);
			
			Path relApkPAth = Paths.get(apkFile.getName().replace(".apk", ""));
			Path absApkPAth = absExtractionPath.resolve(relApkPAth);
			
			JarExtractor.extractAllJars(absApkPAth, DISPLAY_OUTPUT);
		} else {
			System.err.println("Warning: " + apkFile + " is not an APK. Skipping file...");
		}
	}
	
	
	/**
	 * Computes and returns the file map for an APK. The key is the relative path to a class file, while its value is the absolute path to a class file.
	 * 
	 * @param apkPath
	 * @return
	 * @throws IOException
	 */
	public static HashMap<Path, Path> getFileMap(final Path apkPath) throws IOException {
		final HashMap<Path, Path> apkMap = new HashMap<Path, Path>();
		Files.walkFileTree(apkPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path absoluteClassPath, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(absoluteClassPath) && !absoluteClassPath.toString().endsWith(".txt")) {
					Path relativeClassPath = apkPath.relativize(absoluteClassPath);
					apkMap.put(relativeClassPath, absoluteClassPath);
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return apkMap;
	}
}
