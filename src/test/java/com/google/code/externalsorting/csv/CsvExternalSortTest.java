package com.google.code.externalsorting.csv;

import javafx.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class CsvExternalSortTest {
	private static final String FILE_CSV = "externalSorting.csv";
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
		
		List<File> sortInBatch = CsvExternalSort.sortInBatch(file, comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, Charset.defaultCharset(), null, false, 1, CSVFormat.DEFAULT);
		
		assertEquals(sortInBatch.size(), 1);
		
		int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, comparator, Charset.defaultCharset(), false, true, CSVFormat.DEFAULT);
		
		assertEquals(mergeSortedFiles, 4);
		
		BufferedReader reader = new BufferedReader(new FileReader(outputfile));
		String readLine = reader.readLine();
		
		assertEquals(readLine, "6,this wont work in other systems,3");
		reader.close();
	}

	@Test
	public void testCVSFormat() throws Exception {
		Map<CSVFormat, Pair<String, String>> map = new HashMap<CSVFormat, Pair<String, String>>(){{
			put(CSVFormat.MYSQL, new Pair<>(FILE_CSV_WITH_TABS, "6   \"this wont work in other systems\"   3"));
			put(CSVFormat.EXCEL.withDelimiter(SEMICOLON), new Pair<>(FILE_CSV_WITH_SEMICOOLONS, "6;this wont work in other systems;3"));
		}};

		for (Map.Entry<CSVFormat, Pair<String, String>> format : map.entrySet()){
			String path = this.getClass().getClassLoader().getResource(format.getValue().getKey()).getPath();

			File file = new File(path);

			outputfile = new File("outputSort1.csv");

			Comparator<CSVRecord> comparator = (op1, op2) -> op1.get(0)
					.compareTo(op2.get(0));

			List<File> sortInBatch = CsvExternalSort.sortInBatch(file, comparator, CsvExternalSort.DEFAULTMAXTEMPFILES, Charset.defaultCharset(), null, false, 1, format.getKey());

			assertEquals(sortInBatch.size(), 1);

			int mergeSortedFiles = CsvExternalSort.mergeSortedFiles(sortInBatch, outputfile, comparator, Charset.defaultCharset(), false, false, format.getKey());

			assertEquals(mergeSortedFiles, 4);

			List<String> lines = Files.readAllLines(outputfile.toPath());

			assertEquals(format.getValue().getValue(), lines.get(0));
		}
	}

	@After
	public void onTearDown() {
		if(outputfile.exists()) {
			outputfile.delete();
		}
	}
}
