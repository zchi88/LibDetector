package com.zchi88.android.libdetector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.zchi88.android.libdetector.libmetadata.LibraryVersion;
import com.zchi88.android.libdetector.utilities.LibSnapshot;
import com.zchi88.android.libdetector.utilities.Timer;

/**
 * The main class for the LibDetector tool.
 * 
 * @author Zhihao Chi
 *
 */
public class Main {
	// The maximum number of threads running concurrently to process the APKs. Optimal size will vary depending on the power of your machine.
	// If your machine experiences noticeable performance drops while running the tool, the number of threads may need to be reduced
	private static final int THREADPOOL_SIZE = 3;
	private static HashMap<Path, ArrayList<LibraryVersion>> libsSnapshot;
	
	
	/**
	 * Display correct usage information for this tool.
	 */
	private static void showHowToUse() {
		System.err.println("Error. One argument(the path to the whitelist library) is expected. Example:");
		System.err.println("java -jar AndroidLibDetector.jar PATH/TO/LIBRARIES/DIRECTORY");
		System.exit(-1);
	}
	
   
  
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 1) {
			showHowToUse();
		}
		
		String libsPath = args[0];
		Path whitelistPath = Paths.get(libsPath);

		// Check to make sure that the library whitelist directory exists.
		File[] whitelistedLibraries = whitelistPath.toFile().listFiles();
		if (whitelistedLibraries == null) {
			System.err.println("The specified whitelist directory does not exist. Please check that the provided path exists.");
			System.err.println("Exiting program.");
			System.exit(-1);
		}
		
		Path relApksPath = Paths.get("Android_APKs");
		
		Path workingDir = Paths.get("").toAbsolutePath();
		
		Path absApksPath = workingDir.resolve(relApksPath);
		
		// Check to make sure that the directory Android APKs directory exists.
		File[] apks = absApksPath.toFile().listFiles();
		if (apks == null) {
			System.err.println("Error: The Android_APKs directory cannot be found.");
			System.err.println("Please make sure that this tool is in the same directory as the Android_APKs directory.");
			System.err.println("Exiting program.");
			System.exit(-1);
		}
		
		long startTime = System.currentTimeMillis();
		
		System.out.println("Starting LibDetector tool.");
		System.out.println("Performing analysis on APKs located at: " + absApksPath);
		System.out.println("Drawing potential library matches from: " + whitelistPath);
		System.out.println("Looking for libraries now...\n");
		
		// Get a "snapshot" of all libraries and their different version in the whitelist
		libsSnapshot = LibSnapshot.getLibsSnapshot(whitelistPath);
		
		// Start a threadpool service to increase processing power
		ExecutorService libDetectorThreads = Executors.newFixedThreadPool(THREADPOOL_SIZE);
		
		// For each file in the Android_APKs directory, get their source code then identify the libraries in them.
		File[] apkFiles = absApksPath.toFile().listFiles();
		
		for (File file : apkFiles) {
			LibDetector detector = new LibDetector(libsSnapshot, file);
			libDetectorThreads.execute(detector);
		}
		
		// Clean up threadpool after threads finish executing
		libDetectorThreads.shutdown();
		
		// Wait for all threads to finish executing before displaying completion message
		libDetectorThreads.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		
		if (libDetectorThreads.isShutdown()) {
			System.out.println("Finished scanning APK's for libraries.");
		}
		
		long endTime = System.currentTimeMillis();
		System.out.println("Processed " + apkFiles.length + " APKs in " + Timer.msToString(endTime-startTime));
		
	}
	
	
	
}
