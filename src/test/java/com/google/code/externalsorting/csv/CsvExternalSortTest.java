package com.google.code.externalsorting.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class CsvExternalSortTest {
	private static final String FILE_CSV = "externalSorting.csv";
	private static final String FILE_UNICODE_CSV = "nonLatinSorting.csv";

	private static final String FILE_CSV_WITH_TABS = "externalSortingTabs.csv";
	private static final String FILE_CSV_WITH_SEMICOOLONS = "externalSortingSemicolon.csv";
	private static final char SEMICOLON = ';';

	File outputfile;

	@Test
	public void testMultiLineFile() throws IOException, ClassNotFoundException {
		String path = this.getClass().getClassLoader().getResource(FILE_CSV).getPath();
		
		File file = new File(path);
		
		outputfile = new File("outputSort1.csv");
		
		Comparator<CSVRecord> comparator = (op1, op2) -> op1.get(0)
				.compareTo(op2.get(0));

		CsvSortOptions sortOptions = new CsvSortOptions
				.Builder(comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, CsvExternalSort.estimateAvailableMemory())
				.charset(Charset.defaultCharset())
				.distinct(false)
				.numHeader(1)
				.skipHeader(true)
				.format(CSVFormat.DEFAULT)
				.build();
		ArrayList<CSVRecord> header = new ArrayList<CSVRecord>();

		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, null, sortOptions, header);
		
		assertEquals(1, sortInBatch.size());
		
		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, true, header);
		
		assertEquals(4, mergeSortedFiles);
		
		BufferedReader reader = new BufferedReader(new FileReader(outputfile));
		String readLine = reader.readLine();

		assertEquals("6,this wont work in other systems,3", readLine);
		reader.close();
	}

	@Test
	public void testNonLatin() throws Exception {
		Field cs = Charset.class.getDeclaredField("defaultCharset");
		cs.setAccessible(true);
		cs.set(null, Charset.forName("windows-1251"));

		String path = this.getClass().getClassLoader().getResource(FILE_UNICODE_CSV).getPath();

		File file = new File(path);

		outputfile = new File("unicode_output.csv");

		Comparator<CSVRecord> comparator = (op1, op2) -> op1.get(0)
				.compareTo(op2.get(0));

		CsvSortOptions sortOptions = new CsvSortOptions
				.Builder(comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, CsvExternalSort.estimateAvailableMemory())
				.charset(StandardCharsets.UTF_8)
				.distinct(false)
				.numHeader(1)
				.skipHeader(true)
				.format(CSVFormat.DEFAULT)
				.build();
		ArrayList<CSVRecord> header = new ArrayList<CSVRecord>();
		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, null, sortOptions, header);

		assertEquals(1, sortInBatch.size());

		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, true, header);

		assertEquals(5, mergeSortedFiles);

		List<String> lines = Files.readAllLines(Paths.get(outputfile.getPath()), StandardCharsets.UTF_8);

		assertEquals("2,זה רק טקסט אחי לקריאה קשה,8", lines.get(0));
		assertEquals("5,هذا هو النص إخوانه فقط من الصعب القراءة,3", lines.get(1));
		assertEquals("6,это не будет работать в других системах,3", lines.get(2));
	}


	@Test
	public void testCVSFormat() throws Exception {
		Map<CSVFormat, Pair> map = new HashMap<CSVFormat, Pair>(){{
			put(CSVFormat.MYSQL, new Pair(FILE_CSV_WITH_TABS, "6   \"this wont work in other systems\"   3"));
			put(CSVFormat.EXCEL.withDelimiter(SEMICOLON), new Pair(FILE_CSV_WITH_SEMICOOLONS, "6;this wont work in other systems;3"));
		}};

		for (Map.Entry<CSVFormat, Pair> format : map.entrySet()){
			String path = this.getClass().getClassLoader().getResource(format.getValue().getFileName()).getPath();

			File file = new File(path);

			outputfile = new File("outputSort1.csv");

			Comparator<CSVRecord> comparator = (op1, op2) -> op1.get(0)
					.compareTo(op2.get(0));

			CsvSortOptions sortOptions = new CsvSortOptions
					.Builder(comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, CsvExternalSort.estimateAvailableMemory())
					.charset(Charset.defaultCharset())
					.distinct(false)
					.numHeader(1)
					.skipHeader(true)
					.format(format.getKey())
					.build();
			ArrayList<CSVRecord> header = new ArrayList<CSVRecord>();
			List<File> sortInBatch = CsvExternalSort.sortInBatch(file,  null, sortOptions, header);

			assertEquals(1, sortInBatch.size());

			int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, false, header);

			assertEquals(4, mergeSortedFiles);

			List<String> lines = Files.readAllLines(outputfile.toPath());

			assertEquals(format.getValue().getExpected(), lines.get(0));
			assertEquals(4, lines.size());
		}
	}

	@Test
	public void testMultiLineFileWthHeader() throws IOException, ClassNotFoundException {
		String path = this.getClass().getClassLoader().getResource(FILE_CSV).getPath();

		File file = new File(path);

		outputfile = new File("outputSort1.csv");

		Comparator<CSVRecord> comparator = (op1, op2) -> op1.get(0)
				.compareTo(op2.get(0));

		CsvSortOptions sortOptions = new CsvSortOptions
				.Builder(comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, CsvExternalSort.estimateAvailableMemory())
				.charset(Charset.defaultCharset())
				.distinct(false)
				.numHeader(1)
				.skipHeader(false)
				.format(CSVFormat.DEFAULT)
				.build();
		ArrayList<CSVRecord> header = new ArrayList<CSVRecord>();
		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, null, sortOptions, header);

		assertEquals(1, sortInBatch.size());

		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, true, header);

		List<String> lines = Files.readAllLines(outputfile.toPath(), sortOptions.getCharset());

		assertEquals("personId,text,ishired", lines.get(0));
		assertEquals("6,this wont work in other systems,3", lines.get(1));
		assertEquals("6,this wont work in other systems,3", lines.get(2));
		assertEquals("7,My Broken Text will break you all,1", lines.get(3));
		assertEquals("8,this is only bro text for hard read,2", lines.get(4));
		assertEquals(5, lines.size());

	}

	@After
	public void onTearDown() {
		if(outputfile.exists()) {
			outputfile.delete();
		}
	}

	private class Pair {
		private String fileName;
		private String expected;

		public Pair(String fileName, String expected) {
			this.fileName = fileName;
			this.expected = expected;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getExpected() {
			return expected;
		}

		public void setExpected(String expected) {
			this.expected = expected;
		}
	}
}
