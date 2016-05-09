package com.zchi88.android.libdetector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import com.zchi88.android.libdetector.libmetadata.LibraryMetadata;
import com.zchi88.android.libdetector.libmetadata.LibraryStats;
import com.zchi88.android.libdetector.libmetadata.LibraryVersion;
import com.zchi88.android.libdetector.utilities.ApkProcessor;
import com.zchi88.android.libdetector.utilities.ClassSignature;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

/**
 * Contains the methods that are used to compare libraries in APK's to versions
 * of libraries in a whitelist, and determine if there are any matches.
 *
 */
public class LibDetector implements Runnable {
	private static final double LEVEN_THRESHHOLD = 0.5;
	private HashMap<Path, ArrayList<LibraryVersion>> libsSnapshot;
	private File apkFile;
	private static String outputFileName = LibraryMetadata.LIB_RESULTS_FILENAME;

	public LibDetector(HashMap<Path, ArrayList<LibraryVersion>> libsSnapshot, File apkFile) {
		this.libsSnapshot = libsSnapshot;
		this.apkFile = apkFile;
	}

	/**
	 * 
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void identifyLibs() throws InterruptedException, IOException {
		Path workingDir = apkFile.toPath().getParent().getParent();
		Path relExtractionPath = Paths.get("Extracted_APKs").resolve(apkFile.getName().replace(".apk", ""));
		Path decompiledApkPath = workingDir.resolve(relExtractionPath).resolve("byteCode");
		Path outputTextPath = workingDir.resolve(relExtractionPath);
		File outputFile = new File(outputFileName);
		Path outputFilePath = outputTextPath.resolve(outputFile.toPath());

		// Only run on the APK if its results haven't already been computed
		if (!outputFilePath.toFile().exists()) {
			System.out.println("Scanning " + apkFile + " now for libraries.");
			ApkProcessor.processApk(apkFile);

			HashMap<Path, ArrayList<LibraryStats>> libMatches = matchVersions(libsSnapshot, decompiledApkPath);
			outputResults(outputTextPath, libMatches);
		}

		// Remove files besides the LIB_RESULTS_FILENAME to conserve hard
		// drive space, since they are no longer needed.
		cleanUp(outputTextPath);
	}

	/**
	 * Tries to find if an APK uses a specific version of a library by looking
	 * for that library's version-exclusive files in the APK. If none are found,
	 * then we look at the function level.
	 * 
	 * @param libsSnapshot2
	 * @param decompiledApkPath
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static HashMap<Path, ArrayList<LibraryStats>> matchVersions(
			HashMap<Path, ArrayList<LibraryVersion>> libsSnapshot, Path decompiledApkPath)
					throws IOException, InterruptedException {
		// A map to store the results of any matching libraries found
		HashMap<Path, ArrayList<LibraryStats>> libMatches = new HashMap<Path, ArrayList<LibraryStats>>();

		// A mapping of all the class files in the APK
		HashMap<Path, Path> filesInApk = ApkProcessor.getFileMap(decompiledApkPath);

		Set<Path> libPaths = libsSnapshot.keySet();

		for (Path path : libPaths) {
			ArrayList<LibraryVersion> libraryVersions;

			// Set an impossible initial levenshtein distance to characters
			// compared ratio. This will keep track of the closest match of a
			// library
			double bestLevToCharRatio = -1.0;
			ArrayList<LibraryVersion> possibleMatches = new ArrayList<>();
			ArrayList<LibraryStats> closestMatch = new ArrayList<>();
			libraryVersions = libsSnapshot.get(path);
			for (LibraryVersion lib : libraryVersions) {
				// All of the newfiles must exist, so threshold set at 1.0
				ArrayList<File> newFiles = lib.getNewFiles();
				if (filesExist(newFiles, filesInApk, 1.0)) {
					// All of the deleted files must not exist
					ArrayList<File> deletedFiles = lib.getDeletedFiles();
					if (filesDeleted(deletedFiles, filesInApk)) {
						// All of the modified files must exist
						ArrayList<File> moddedFiles = lib.getModdedFiles();
						if (filesExist(moddedFiles, filesInApk, 1.0)) {
							// Allow for some copied files to be missing, so
							// long as the other above conditions are met.
							ArrayList<File> copiedFiles = lib.getCopiedFiles();
							if (filesExist(copiedFiles, filesInApk, 0.8)) {
								possibleMatches.add(lib);
							}

						}

					}
				}
			}

			if (possibleMatches.size() == 1) {
				LibraryVersion lib = possibleMatches.get(0);
				LibraryStats libStats = new LibraryStats(path.toString(), lib.getVersionName().toString(),
						lib.getVersionAge(), 0.0);
				closestMatch.add(libStats);
			} else {
				for (LibraryVersion potMatch : possibleMatches) {
					double levenshteinDist = 0;
					double totalChars = 0;
					ArrayList<File> moddedFiles = potMatch.getModdedFiles();
					for (File file : moddedFiles) {
						// Get the full file path for the class file in
						// the library
						File libClassFile = potMatch.getVersionPath().resolve(file.toPath()).toFile();
						// Get the full file path for the corresponding
						// class file in the APK
						File apkClassFile = filesInApk.get(file.toPath()).toFile();

						// Compute their class signature using javap

						String libClassSig = ClassSignature.getClassSignature(libClassFile);
						String apkClassSig = ClassSignature.getClassSignature(apkClassFile);

						// Get the longer string of the two, and add it
						// to the running total "totalChars"
						double charCount = (libClassSig.length() > apkClassSig.length()) ? libClassSig.length()
								: apkClassSig.length();
						totalChars += charCount;

						// Create a new diff object, and find the diffs
						// between the class signatures
						diff_match_patch classDiff = new diff_match_patch();
						LinkedList<Diff> diffResult = classDiff.diff_main(libClassSig, apkClassSig);

						// Convert that diff into a Levenshtein distance
						double tempLev = classDiff.diff_levenshtein(diffResult);
						levenshteinDist += tempLev;
					}

					double tempRatio = (levenshteinDist / totalChars);

					if (tempRatio < LEVEN_THRESHHOLD) {
						// Check to see if the ratio is 0, meaning a
						// perfect match. Since we are checking in
						// chronological order, it should be okay to
						// stop looking through other versions when this
						// happens, since we are interested in the
						// newest possible version a library in an APK
						// can be.
						if (tempRatio == 0.0) {
							closestMatch.clear();
							LibraryStats libStats = new LibraryStats(path.toString(),
									potMatch.getVersionName().toString(), potMatch.getVersionAge(), tempRatio);
							closestMatch.add(libStats);
							break;
						}

						if (bestLevToCharRatio == -1) {
							bestLevToCharRatio = tempRatio;
							LibraryStats libStats = new LibraryStats(path.toString(),
									potMatch.getVersionName().toString(), potMatch.getVersionAge(), bestLevToCharRatio);
							closestMatch.add(libStats);
						} else {
							if (tempRatio == bestLevToCharRatio) {
								LibraryStats libStats = new LibraryStats(path.toString(),
										potMatch.getVersionName().toString(), potMatch.getVersionAge(),
										bestLevToCharRatio);
								closestMatch.add(libStats);
							}
							if (tempRatio < bestLevToCharRatio) {
								bestLevToCharRatio = tempRatio;
								LibraryStats libStats = new LibraryStats(path.toString(),
										potMatch.getVersionName().toString(), potMatch.getVersionAge(),
										bestLevToCharRatio);
								closestMatch.clear();
								closestMatch.add(libStats);
							}
						}

					}
				}
			}

			// return closestMatch;
			if (!closestMatch.isEmpty()) {
				libMatches.put(path, closestMatch);
				// System.err.println("Match found! " +
				// closestMatch.get(0).getVersionName());
			} else {
				// System.out.println("This app does not use " +
				// path.getFileName());
			}
		}

		return libMatches;
	}

	/**
	 * Simply returns true if and only if all the files that are in the
	 * filesList list exist in the APK above the given threshold. A threshold of
	 * 1.0 means that all the files must be present.
	 * 
	 * @param newFiles
	 * @param filesInApk
	 * @return
	 */
	private static boolean filesExist(ArrayList<File> filesList, HashMap<Path, Path> filesInApk, double threshold) {
		if (!filesList.isEmpty()) {
			double apkCount = 0;

			for (File libFile : filesList) {
				// If ever a null value is found for a file in the APK map, then
				// we know that it does not exist
				if (filesInApk.get(libFile.toPath()) != null) {
					apkCount++;
				}
			}
			return (apkCount >= filesList.size() * threshold);
		}

		return true;
	}

	/**
	 * Simply returns true if and only if all the files that are in the
	 * deletedFiles list do not exist in the APK
	 * 
	 * @param deletedFiles
	 * @param filesInApk
	 * @return
	 */
	private static boolean filesDeleted(ArrayList<File> deletedFiles, HashMap<Path, Path> filesInApk) {
		if (!deletedFiles.isEmpty()) {
			for (File libFile : deletedFiles) {
				// We expect all values to be null, otherwise the file does
				// exist
				if (filesInApk.get(libFile.toPath()) != null) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Takes a hashmap of results and prints it out to a text file.
	 * 
	 * @param outputPath
	 * @param outputData
	 * @throws IOException
	 */
	private static void outputResults(Path outputPath, HashMap<Path, ArrayList<LibraryStats>> outputData)
			throws IOException {
		File outputFile = new File("libraryMatchResults.txt");
		Path outputFilePath = outputPath.resolve(outputFile.toPath());
		Set<Path> libraries = outputData.keySet();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath.toFile()));) {
			writer.write("Results for: " + outputPath.getFileName());
			writer.newLine();
			// Todo: retrieve release date information from apps database
			writer.write("Release date: ");
			writer.newLine();
			// Todo: retrieve developer email from apps database
			writer.write("Developer email: ");
			writer.newLine();
			writer.newLine();
			writer.write("Key: LIBRARY_NAME, VERSION_AGE, LEVENSHTEIN_RATIO");
			writer.newLine();
			writer.newLine();
			writer.newLine();
			if (outputData.isEmpty()) {
				writer.write("No matches to libraries in current whitelist found.");
				writer.newLine();
			} else {
				for (Path library : libraries) {
					ArrayList<LibraryStats> libMatches = outputData.get(library);
					writer.write(library.toString().toUpperCase() + ": " + libMatches.size() + " potential match(es):");
					writer.newLine();
					for (LibraryStats libraryStats : libMatches) {
						writer.write("	" + libraryStats.getVersionName() + ", " + libraryStats.getVersionAge() + ", "
								+ libraryStats.getLevenshteinToCharRatio());
						writer.newLine();
					}
					writer.newLine();
				}
			}
		}
	}
	

	/**
	 * Cleans up the files that were generated in order to produce the library
	 * match results. These files are: the APK's classes.dex files, the
	 * classes.jar files, and the all files in the byteCode folder. 
	 * 
	 * @param pathToClean
	 */
	private static void cleanUp(Path pathToClean) {
		System.out.println("Cleaning up temporary files in " + pathToClean + ".");
		File resultsFile = pathToClean.resolve(outputFileName).toFile();

		if (resultsFile.exists()) {
			File[] filesList = pathToClean.toFile().listFiles();
			for (File file : filesList) {
				if (!file.equals(resultsFile)) {
					// Call the recursive delete method to delete the folder and all of its contents, since a non-empty folder cannot be deleted
					recursiveDelete(file);
				}
			}
		}
	}

	
	/**
	 * Used to recursively delete the contents of a directory. Meant specifically for deleting non-empty folders.
	 * 
	 * @param filePath
	 */
	private static void recursiveDelete(File filePath) {
		if (filePath.isDirectory()) {
			File[] filesList = filePath.listFiles();
			for (File file : filesList) {
				recursiveDelete(file);
			}
		}

		filePath.delete();
	}

	public void run() {
		try {
			identifyLibs();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
