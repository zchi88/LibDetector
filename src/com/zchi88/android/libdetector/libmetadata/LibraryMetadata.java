package com.zchi88.android.libdetector.libmetadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contains methods to analyze the library results metadata after the
 * LibDetector has finished scanning APKs for libraries. This makes analysis of
 * the libraries being used in APKs easier.
 *
 */
public class LibraryMetadata {
	public static final String LIB_RESULTS_FILENAME = "libraryMatchResults.txt";

	/**
	 * Computes the metadata by creating a Hashmap representing library result
	 * metadata, and keeping track of the number of occurences of a library in
	 * an APK in that hashmap. 
	 * 
	 * @param extractionPath
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, LibraryStats> computeMetadata(Path extractionPath) throws IOException {
		System.out.println("Computing metadata for libraries found in APKs...");

		File[] apksList = extractionPath.toFile().listFiles();
		String resultFileName = LIB_RESULTS_FILENAME;

		HashMap<String, LibraryStats> libMetadata = new HashMap<>();

		// For each apk, look through its LIB_RESULTS_FILENAME and start
		// tallying up the number of occurrences of a library. Store that
		// metadata in the libMetadata Hashmap.
		for (File apkFile : apksList) {
			Path resultFilePath = apkFile.toPath().resolve(resultFileName);

			// Make sure the LIB_RESULTS_FILENAME file exists
			if (resultFilePath.toFile().exists()) {
				try(Scanner scanner = new Scanner(resultFilePath)) {
					String nextLine;
					while (scanner.hasNextLine()) {
						nextLine = scanner.nextLine();
						// All library results in LIB_RESULTS_FILENAME start with
						// a tab
						if (nextLine.startsWith("	")) {
							// Strip all whitespace from the string
							nextLine = nextLine.trim().replace(" ", "");
							String[] vals = nextLine.split(",");

							String libName = vals[0];

							LibraryStats libraryStats;

							libraryStats = libMetadata.get(libName);

							// We are mostly just interested in the version name of
							// the library, its version age and how many times it
							// was used in an APK. The other fields don't matter.
							if (libraryStats == null) {
								int versionAge = Integer.parseInt(vals[1]);
								libraryStats = new LibraryStats(libName, libName, versionAge, 0.0);
								libMetadata.put(libName, libraryStats);
							} else {
								libraryStats.incrementLibCount();
							}
						}
					}
				} 
			}
		}
		System.out.println("Done.");
		return libMetadata;
	}

	/**
	 * Writes the metadata to a text file.
	 * 
	 * @param inputData
	 * @param outputFile
	 * @throws IOException
	 */
	public static void metadataToFile(HashMap<String, LibraryStats> inputData, File outputFile) throws IOException {
		System.out.println("Writing metadata results to " + outputFile);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			Set<String> libsInMap = inputData.keySet();
			TreeSet<String> sortedLibs = new TreeSet<>(libsInMap);

			for (String libName : sortedLibs) {
				LibraryStats libraryStats = inputData.get(libName);
				writer.write(libName + ": Found in " + libraryStats.getLibCount() + " APK(s).");
				writer.newLine();
				writer.write("Versions Behind: " + libraryStats.getVersionAge());
				writer.newLine();
				writer.newLine();
			}
		}

		System.out.println("Done.");
	}
}
