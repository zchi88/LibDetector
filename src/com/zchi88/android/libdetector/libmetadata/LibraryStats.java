package com.zchi88.android.libdetector.libmetadata;

public class LibraryStats {
	public LibraryStats(String libName, String versionName, int versionAge, double levRatio){
		this.libName = libName;
		this.versionName = versionName;
		this.versionAge = versionAge;
		this.levenshteinToCharRatio = levRatio;
		this.libCount = 1;
	}
	
	private String libName;
	
	private String versionName;
	
	private int libCount;
	
	private int versionAge;
	
	private double levenshteinToCharRatio;
	
	
	
	
	public String getLibName() {
		return libName;
	}

	public String getVersionName() {
		return versionName;
	}

	public int getLibCount() {
		return libCount;
	}
	
	public void incrementLibCount() {
		libCount++;
	}


	public int getVersionAge() {
		return versionAge;
	}

	public double getLevenshteinToCharRatio() {
		return levenshteinToCharRatio;
	}
	
}
