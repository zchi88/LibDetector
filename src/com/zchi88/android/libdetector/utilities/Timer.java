package com.zchi88.android.libdetector.utilities;

public class Timer {
	/**
	 * Given an elapsed time in milliseconds, returns a human readable elapsed time.
	 * 
	 * 
	 * @param ms
	 * @return
	 */
    public static String msToString(long ms) {
        long totalSecs = ms/1000;
        long hours = (totalSecs / 3600);
        long mins = (totalSecs / 60) % 60;
        long secs = totalSecs % 60;
        String minsString = (mins == 0)
            ? "00"
            : ((mins < 10)
               ? "0" + mins
               : "" + mins);
        String secsString = (secs == 0)
            ? "00"
            : ((secs < 10)
               ? "0" + secs
               : "" + secs);
        if (hours > 0)
            return hours + " hour(s), " + minsString + " minute(s) and " + secsString + " second(s).";
        else if (mins > 0)
            return mins + " minute(s) and " + secsString + " second(s).";
        else return "" + secsString + " second(s).";
    }
}
