package com.zchi88.android.libdetector.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Contains methods for computing a class "signature", which consists of all the
 * method and field declarations of a class.
 * 
 *
 */
public class ClassSignature {
	/**
	 * Given a .class file, returns its signature, which is a string of its
	 * sorted method and field declarations.
	 * 
	 * @param classFile
	 * @return
	 * @throws IOException
	 */
	public static String getClassSignature(File classFile) throws IOException {
		String pathToClassFile = classFile.toString();

		ProcessBuilder pb = new ProcessBuilder("javap", "-private", pathToClassFile);
		Process p = pb.start();
		ArrayList<String> classContent = new ArrayList<>();

		// Add the output of javap to an ArrayList first so that we can sort the
		// methods and field declarations
		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line;
			while ((line = br.readLine()) != null) {
				// Ignore the "compiled from" extra line of information that is
				// sometimes given by a class file
				if (!line.startsWith("Compiled from")) {
					// Remove the synchronized keyword if present to improve
					// Levenshtein distance results, since many APK compilers
					// discard it
					line = line.replace("synchronized ", "");
					classContent.add(line.trim());
				}
			}
		}

		Collections.sort(classContent);

		// Build a string which then represents the sorted method and field
		// declarations in a .class file
		StringBuilder sb = new StringBuilder();

		for (String string : classContent) {
			sb.append(string);
		}

		return sb.toString();
	}
}
