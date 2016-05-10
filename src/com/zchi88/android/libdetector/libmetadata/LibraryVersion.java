package com.zchi88.android.libdetector.libmetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import com.zchi88.android.libdetector.utilities.DiffParser;


/**
 * The purpose of this class is to organize and store information about a
 * version of an android library, such as the files that are new to it or
 * are uniquely modified. It relies on the diff parser to populate its fields.
 * 
 */
public class LibraryVersion {
	public LibraryVersion(final Path versionPath, String machineOS) throws IOException {
		this.versionName = versionPath.getFileName();
		this.versionPath = versionPath;
		
		File diffFileName = new File("diff.txt");
		Path diffPath = versionPath.resolve(diffFileName.toPath());
		
		this.versionAge = DiffParser.getVersionAge(diffPath.toFile());
		this.newFiles = DiffParser.getNewFiles(diffPath.toFile(), machineOS);
		this.moddedFiles = DiffParser.getModdedFiles(diffPath.toFile(), machineOS);
		this.deletedFiles = DiffParser.getDeletedFiles(diffPath.toFile(), machineOS);
		this.copiedFiles = DiffParser.getCopiedFiles(diffPath.toFile(), machineOS);
	}

	// The name of the version of the library
	private final Path versionName;

	// The path to the folder holding the decompiled version source code
	private final Path versionPath;

	// The "age" of the version. For example, 0 would mean it is the most recent. 2 would mean it is 2 versions behind.
	private final int versionAge;


	// A list of files in that version not found in any other version
	private final ArrayList<File> newFiles;

	// A list of files in that version that exist in other versions, but with
	// different content
	private final ArrayList<File> moddedFiles;
	
	// A list of files that have been removed since the previous version
	private final ArrayList<File> deletedFiles;
	
	// A list of files that have been removed since the previous version
	private final ArrayList<File> copiedFiles;

	public Path getVersionName() {
		return versionName;
	}

	public Path getVersionPath() {
		return versionPath;
	}
	
	public int getVersionAge() {
		return versionAge;
	}

	public ArrayList<File> getNewFiles() {
		return newFiles;
	}

	public ArrayList<File> getModdedFiles() {
		return moddedFiles;
	}
	
	public ArrayList<File> getDeletedFiles() {
		return deletedFiles;
	}
	
	public ArrayList<File> getCopiedFiles() {
		return copiedFiles;
	}
}
