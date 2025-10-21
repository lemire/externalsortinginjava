package com.google.code.externalsorting.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * Utility class for performing external sorting on CSV files.
 * Provides methods to sort large CSV files efficiently using external memory.
 */
public class CsvExternalSort {

	private static final Logger LOG = Logger.getLogger(CsvExternalSort.class.getName());

	private CsvExternalSort() {
		throw new UnsupportedOperationException("Unable to instantiate utility class");
	}

	/**
	 * This method calls the garbage collector and then returns the free memory.
	 * This avoids problems with applications where the GC hasn't reclaimed memory
	 * and reports no available memory.
	 * 
	 * @return available memory
	 */
	public static long estimateAvailableMemory() {
		System.gc();
		return Runtime.getRuntime().freeMemory();
	}

	/**
	 * we divide the file into small blocks. If the blocks are too small, we shall
	 * create too many temporary files. If they are too big, we shall be using too
	 * much memory.
	 * 
	 * @param sizeoffile  how much data (in bytes) can we expect
	 * @param maxtmpfiles how many temporary files can we create (e.g., 1024)
	 * @param maxMemory   Maximum memory to use (in bytes)
	 * @return the estimate
	 */
	public static long estimateBestSizeOfBlocks(final long sizeoffile, final int maxtmpfiles, final long maxMemory) {
		// we don't want to open up much more than maxtmpfiles temporary
		// files, better run
		// out of memory first.
		long blocksize = sizeoffile / maxtmpfiles + (sizeoffile % maxtmpfiles == 0 ? 0 : 1);

		// on the other hand, we don't want to create many temporary
		// files
		// for naught. If blocksize is smaller than half the free
		// memory, grow it.
		if (blocksize < maxMemory / 6) {
			blocksize = maxMemory / 6;
		}
		return blocksize;
	}

    /**
     * Merges multiple sorted CSVRecordBuffer objects into a single output file.
     * @param fbw the BufferedWriter for output
     * @param sortOptions sorting options
     * @param bfbs list of CSVRecordBuffer objects
     * @param header list of header records
     * @return the number of records written
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static int mergeSortedFiles(BufferedWriter fbw, final CsvSortOptions sortOptions, List<CSVRecordBuffer> bfbs, List<CSVRecord> header)
	    throws IOException, ClassNotFoundException {
		PriorityQueue<CSVRecordBuffer> pq = new PriorityQueue<CSVRecordBuffer>(11, new Comparator<CSVRecordBuffer>() {
			@Override
			public int compare(CSVRecordBuffer i, CSVRecordBuffer j) {
				return sortOptions.getComparator().compare(i.peek(), j.peek());
			}
		});
		for (CSVRecordBuffer bfb : bfbs)
			if (!bfb.empty())
				pq.add(bfb);
		int numWrittenLines = 0;
		CSVPrinter printer = new CSVPrinter(fbw, sortOptions.getFormat());
		if(! sortOptions.isSkipHeader()) {
			for(CSVRecord r: header) {
				printer.printRecord(r);
			}
		}
		CSVRecord lastLine = null;
		try {
			while (pq.size() > 0) {
				CSVRecordBuffer bfb = pq.poll();
				CSVRecord r = bfb.pop();
				// Skip duplicate lines
				if (sortOptions.isDistinct() && checkDuplicateLine(r, lastLine)) {
				} else {
					printer.printRecord(r);
					lastLine = r;
					++numWrittenLines;
				}
				if (bfb.empty()) {
					bfb.close();
				} else {
					pq.add(bfb); // add it back
				}
			}
		} finally {
			printer.close();
			fbw.close();
			for (CSVRecordBuffer bfb : pq)
				bfb.close();
		}

		return numWrittenLines;
	}

    /**
     * Merges multiple sorted CSV files into a single output file.
     * @param files list of sorted files
     * @param outputfile the output file
     * @param sortOptions sorting options
     * @param append whether to append to the output file
     * @param header list of header records
     * @return the number of records written
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static int mergeSortedFiles(List<File> files, File outputfile, final CsvSortOptions sortOptions,
	    boolean append, List<CSVRecord> header) throws IOException, ClassNotFoundException {

		List<CSVRecordBuffer> bfbs = new ArrayList<CSVRecordBuffer>();
		for (File f : files) {
			InputStream in = new FileInputStream(f);
			BufferedReader fbr = new BufferedReader(new InputStreamReader(in, sortOptions.getCharset()));
			CSVParser parser = new CSVParser(fbr, sortOptions.getFormat());
			CSVRecordBuffer bfb = new CSVRecordBuffer(parser);
			bfbs.add(bfb);
		}

		BufferedWriter fbw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputfile, append), sortOptions.getCharset()));

		int numWrittenLines = mergeSortedFiles(fbw, sortOptions, bfbs, header);
		for (File f : files) {
			if (!f.delete()) {
				LOG.log(Level.WARNING, String.format("The file %s was not deleted", f.getName()));
			}
		}

		return numWrittenLines;
	}

    /**
     * Sorts records in batches and saves them to temporary files.
     * @param size_in_byte the size of the batch in bytes
     * @param fbr the BufferedReader for input
     * @param tmpdirectory the directory for temporary files
     * @param sortOptions sorting options
     * @param header list of header records
     * @return list of temporary files containing sorted batches
     * @throws IOException if an I/O error occurs
     */
    public static List<File> sortInBatch(long size_in_byte, final BufferedReader fbr, final File tmpdirectory,
	    final CsvSortOptions sortOptions, List<CSVRecord> header) throws IOException {

		List<File> files = new ArrayList<File>();
		long blocksize = estimateBestSizeOfBlocks(size_in_byte, sortOptions.getMaxTmpFiles(),
				sortOptions.getMaxMemory());// in
		// bytes
		AtomicLong currentBlock = new AtomicLong(0);
		List<CSVRecord> tmplist = new ArrayList<CSVRecord>();

		try (CSVParser parser = new CSVParser(fbr, sortOptions.getFormat())) {
			parser.spliterator().forEachRemaining(e -> {
				if (e.getRecordNumber() <= sortOptions.getNumHeader()) {
					header.add(e);
				} else {
					tmplist.add(e);
					currentBlock.addAndGet(SizeEstimator.estimatedSizeOf(e));
				}
				if (currentBlock.get() >= blocksize) {
					try {
						files.add(sortAndSave(tmplist, tmpdirectory, sortOptions));
					} catch (IOException e1) {
						LOG.log(Level.WARNING, String.format("Error during the sort in batch"), e1);
					}
					tmplist.clear();
					currentBlock.getAndSet(0);
				}
			});
		}
		if (!tmplist.isEmpty()) {
			files.add(sortAndSave(tmplist, tmpdirectory, sortOptions));
		}

		return files;
	}

	/**
	 * Sorts a list of CSVRecord objects and saves them to a temporary file.
	 * @param tmplist the list of CSVRecord objects to sort
	 * @param tmpdirectory the directory for temporary files
	 * @param sortOptions sorting options
	 * @return the temporary file containing sorted records
	 * @throws IOException if an I/O error occurs
	 */
	public static File sortAndSave(List<CSVRecord> tmplist, File tmpdirectory, final CsvSortOptions sortOptions) throws IOException {
		Collections.sort(tmplist, sortOptions.getComparator());
		File newtmpfile = File.createTempFile("sortInBatch", "flatfile", tmpdirectory);
		newtmpfile.deleteOnExit();

		CSVRecord lastLine = null;
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(newtmpfile), sortOptions.getCharset());
				CSVPrinter printer = new CSVPrinter(new BufferedWriter(writer), sortOptions.getFormat());) {
			for (CSVRecord r : tmplist) {
				// Skip duplicate lines
				if (sortOptions.isDistinct() && checkDuplicateLine(r, lastLine)) {
				} else {
					printer.printRecord(r);
					lastLine = r;
				}
			}
		}

		return newtmpfile;
	}

	private static boolean checkDuplicateLine(CSVRecord currentLine, CSVRecord lastLine) {
		if (lastLine == null || currentLine == null) {
			return false;
		}

		for (int i = 0; i < currentLine.size(); i++) {
			if (!currentLine.get(i).equals(lastLine.get(i))) {
				return false;
			}
		}
		return true;
	}

    /**
     * Sorts records from a file in batches and saves them to temporary files.
     * @param file the input file
     * @param tmpdirectory the directory for temporary files
     * @param sortOptions sorting options
     * @param header list of header records
     * @return list of temporary files containing sorted batches
     * @throws IOException if an I/O error occurs
     */
    public static List<File> sortInBatch(File file, File tmpdirectory, final CsvSortOptions sortOptions, List<CSVRecord> header)
	    throws IOException {
		try (BufferedReader fbr = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), sortOptions.getCharset()))) {
			return sortInBatch(file.length(), fbr, tmpdirectory, sortOptions, header);
		}
	}

	/**
	 * Default maximal number of temporary files allowed.
	 */
	public static final int DEFAULTMAXTEMPFILES = 1024;

}
