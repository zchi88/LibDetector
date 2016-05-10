package com.zchi88.android.libdetector.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import com.zchi88.android.libdetector.libmetadata.LibraryVersion;

public class LibSnapshot {
	/**
     * Get a "snapshot" of all libraries in the whitelist at runtime. 
     * 
     * @param pathToWhitelist
     * @return A hashmap which maps the path to a library to an ArrayList of its LibraryVersion objects
     * @throws IOException
     */
	public static HashMap<Path, ArrayList<LibraryVersion>> getLibsSnapshot(Path pathToWhitelist) throws IOException{
		HashMap<Path, ArrayList<LibraryVersion>> snapShot= new HashMap<Path, ArrayList<LibraryVersion>>();

		
		File[] libsInWhitelist = pathToWhitelist.toFile().listFiles();
		for (File library : libsInWhitelist) {
			if (library.isDirectory()) {
				String libName = library.getName();
				ArrayList<LibraryVersion> libraryVersions = new ArrayList<LibraryVersion>();
				File[] lib = library.listFiles();
				for (File version : lib) {
					if (version.isDirectory()) {
						File diffFileName = new File("diff.txt");
						Path diffPath = version.toPath().resolve(diffFileName.toPath());
						if (diffPath.toFile().exists()) {
							String machineOS = System.getProperty("os.name").toLowerCase();

							LibraryVersion libVersion = new LibraryVersion(version.toPath(), machineOS);
							
							// Ensures that the libraries in the snapshot are in chronological order, with the most recent being first
							if (libraryVersions.size() == 0) {
								libraryVersions.add(libVersion);
							} else {
								int insertIndex = 0;
								while (insertIndex < libraryVersions.size() && libVersion.getVersionAge() > libraryVersions.get(insertIndex).getVersionAge()) {
									insertIndex++;
								}
								libraryVersions.add(insertIndex, libVersion);
							}
						}
					}
				}
				if (!libraryVersions.isEmpty()) {
					snapShot.put(Paths.get(libName), libraryVersions);
				}
			}
		}
		return snapShot;
	}
}
