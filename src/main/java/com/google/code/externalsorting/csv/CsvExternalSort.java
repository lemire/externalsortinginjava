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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

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
		int rowcounter = 0;
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
				}
				++rowcounter;
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

		return rowcounter;
	}

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

		int rowcounter = mergeSortedFiles(fbw, sortOptions, bfbs, header);
		for (File f : files) {
			if (!f.delete()) {
				LOG.log(Level.WARNING, String.format("The file %s was not deleted", f.getName()));
			}
		}

		return rowcounter;
	}

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
