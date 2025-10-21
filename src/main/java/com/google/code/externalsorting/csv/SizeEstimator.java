package com.google.code.externalsorting.csv;

/**
 * Utility class for estimating the memory size of objects and arrays in the JVM.
 * This is used to approximate the memory usage of CSV records and related structures.
 */
public final class SizeEstimator {
	private static int OBJ_HEADER;
	private static int ARR_HEADER;
	private static int INT_FIELDS = 12;
	private static int OBJ_REF;
	private static int OBJ_OVERHEAD;
	private static boolean IS_64_BIT_JVM;

	/**
     * Private constructor to prevent instantiation.
     */
	private SizeEstimator() {
	}

	/**
     * Class initializations.
     */
	static {
		// By default we assume 64 bit JVM
		// (defensive approach since we will get
		// larger estimations in case we are not sure)
		IS_64_BIT_JVM = true;
		// check the system property "sun.arch.data.model"
		// not very safe, as it might not work for all JVM implementations
		// nevertheless the worst thing that might happen is that the JVM is 32bit
		// but we assume its 64bit, so we will be counting a few extra bytes per string object
		// no harm done here since this is just an approximation.
		String arch = System.getProperty("sun.arch.data.model");
		if (arch != null) {
			if (arch.indexOf("32") != -1) {
				// If exists and is 32 bit then we assume a 32bit JVM
				IS_64_BIT_JVM = false;
			}
		}
		// The sizes below are a bit rough as we don't take into account 
		// advanced JVM options such as compressed oops
		// however if our calculation is not accurate it'll be a bit over
		// so there is no danger of an out of memory error because of this.
		OBJ_HEADER = IS_64_BIT_JVM ? 16 : 8;
		ARR_HEADER = IS_64_BIT_JVM ? 24 : 12;
		OBJ_REF = IS_64_BIT_JVM ? 8 : 4;
		OBJ_OVERHEAD = OBJ_HEADER + INT_FIELDS + OBJ_REF + ARR_HEADER;

	}
	
	/**
	 * Estimates the size of a object in bytes.
	 * 
	 * @param s The string to estimate memory footprint.
	 * @return The <strong>estimated</strong> size in bytes.
	 */
	public static long estimatedSizeOf(Object s) {
		return ((long) (s.toString().length() * 2) + OBJ_OVERHEAD);
	}
}
