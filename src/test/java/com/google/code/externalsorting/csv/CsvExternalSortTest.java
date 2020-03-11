package com.google.code.externalsorting.csv;

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
import java.util.List;

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
				.Builder(CsvExternalSort.DEFAULTMAXTEMPFILES, comparator, 1, CsvExternalSort.estimateAvailableMemory())
				.charset(Charset.defaultCharset())
				.distinct(false)
				.numHeader(1, CSVFormat.DEFAULT)
				.build();

		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, null, sortOptions);
		
		assertEquals(sortInBatch.size(), 1);
		
		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, true, CSVFormat.DEFAULT);
		
		assertEquals(mergeSortedFiles, 4);
		
		BufferedReader reader = new BufferedReader(new FileReader(outputfile));
		String readLine = reader.readLine();

		assertEquals(readLine, "6,this wont work in other systems,3");
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
				.Builder(CsvExternalSort.DEFAULTMAXTEMPFILES, comparator, 1, CsvExternalSort.estimateAvailableMemory())
				.charset(StandardCharsets.UTF_8)
				.distinct(false)
				.numHeader(1, CSVFormat.DEFAULT)
				.build();

		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, null, sortOptions);

		assertEquals(sortInBatch.size(), 1);

		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, true, CSVFormat.DEFAULT);

		assertEquals(mergeSortedFiles, 5);

		List<String> lines = Files.readAllLines(Paths.get(outputfile.getPath()), StandardCharsets.UTF_8);

		assertEquals(lines.get(0), "2,זה רק טקסט אחי לקריאה קשה,8");
		assertEquals(lines.get(1), "5,هذا هو النص إخوانه فقط من الصعب القراءة,3");
		assertEquals(lines.get(2), "6,это не будет работать в других системах,3");
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

			List<File> sortInBatch = CsvExternalSort.sortInBatch(file, comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, Charset.defaultCharset(), null, false, 1, format.getKey());

			assertEquals(sortInBatch.size(), 1);

			int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, comparator, Charset.defaultCharset(), false, false, format.getKey());

			assertEquals(mergeSortedFiles, 4);

			List<String> lines = Files.readAllLines(outputfile.toPath());

			assertEquals(format.getValue().getExpected(), lines.get(0));
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
				.Builder(CsvExternalSort.DEFAULTMAXTEMPFILES, comparator, 1, CsvExternalSort.estimateAvailableMemory())
				.charset(Charset.defaultCharset())
				.distinct(false)
				.numHeader(1)
				.skipHeader(false)
				.build();

		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, null, sortOptions);

		assertEquals(sortInBatch.size(), 1);

		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, sortOptions, true);

		assertEquals(mergeSortedFiles, 5);

		List<String> lines = Files.readAllLines(outputfile.toPath(), sortOptions.getCharset());

		assertEquals(lines.get(0), "personId,text,ishired");
		assertEquals(lines.get(1), "6,this wont work in other systems,3");
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
