package com.google.code.externalsorting;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.github.jamm.*;

/**
 * Unit test for simple App.
 */
@SuppressWarnings({"static-method","javadoc"})
public class ExternalSortTest {
    private static final String TEST_FILE1_TXT = "test-file-1.txt";
    private static final String TEST_FILE2_TXT = "test-file-2.txt";
    private static final String TEST_FILE1_CSV = "test-file-1.csv";
    private static final String[] EXPECTED_SORT_RESULTS = { "a", "b", "b", "e", "f",
                                                            "i", "m", "o", "u", "u", "x", "y", "z"
                                                          };
    private static final String[] EXPECTED_MERGE_RESULTS = {"a", "a", "b", "c", "c", "d", "e", "e", "f", "g", "g","h", "i", "j", "k"};
    private static final String[] EXPECTED_MERGE_DISTINCT_RESULTS = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"};
    private static final String[] EXPECTED_HEADER_RESULTS = {"HEADER, HEADER", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"};
    private static final String[] EXPECTED_DISTINCT_RESULTS = { "a", "b", "e",
                                                                "f", "i", "m", "o", "u", "x", "y", "z"
                                                              };
    private static final String[] SAMPLE = { "f", "m", "b", "e", "i", "o", "u",
                                             "x", "a", "y", "z", "b", "u"
                                           };

    private File file1;
    private File file2;
    private File csvFile;
    private List<File> fileList;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        this.fileList = new ArrayList<File>(3);
        this.file1 = new File(this.getClass().getClassLoader()
                              .getResource(TEST_FILE1_TXT).toURI());
        this.file2 = new File(this.getClass().getClassLoader()
                              .getResource(TEST_FILE2_TXT).toURI());
        this.csvFile = new File(this.getClass().getClassLoader()
                                .getResource(TEST_FILE1_CSV).toURI());

        File tmpFile1 = new File(this.file1.getPath().toString()+".tmp");
        File tmpFile2 = new File(this.file2.getPath().toString()+".tmp");

        copyFile(this.file1, tmpFile1);
        copyFile(this.file2, tmpFile2);

        this.fileList.add(tmpFile1);
        this.fileList.add(tmpFile2);
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        this.file1 = null;
        this.file2 = null;
        this.csvFile = null;
        for(File f:this.fileList) {
            f.delete();
        }
        this.fileList.clear();
        this.fileList = null;
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileInputStream fis = new FileInputStream(sourceFile);
                FileChannel source = fis.getChannel();
                FileOutputStream fos = new FileOutputStream(destFile);
                FileChannel destination = fos.getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }

    }

    public static int estimateTotalSize(String[] mystrings) {
      int total = 0;
      for (String s : mystrings) {
        total += StringSizeEstimator.estimatedSizeOf(s);
      }
      return total;
    }

    public static void oneRoundOfStringSizeEstimation() {
      // could use JMH for better results but this should do
      final int N = 1024;
      String [] mystrings = new String[1024];
      for(int k = 0; k < N ; ++k ) {
        mystrings[k] = Integer.toString(k);
      }
      final int repeat = 1000;
      long bef, aft, diff;
      long bestdiff = Long.MAX_VALUE;
      int bogus = 0;
      for(int t = 0 ; t < repeat; ++t ) {
        bef = System.nanoTime();
        bogus += estimateTotalSize(mystrings);
        aft = System.nanoTime();
        diff = aft - bef;
        if(diff < bestdiff) bestdiff = diff;
      }
      System.out.println("#ignore = "+bogus);
      System.out.println("[performance] String size estimator uses "+bestdiff * 1.0 / N + " ns per string");
    }

    /**
    * This checks that the estimation is reasonably accurate.
    */
    @Test
    public void stringSizeEstimatorQuality() {
      MemoryMeter meter = new MemoryMeter().ignoreKnownSingletons().ignoreOuterClassReference().ignoreNonStrongReferences();
      for(int k = 0; k < 100; ++k) {
        String s = new String();
        while(s.length() < k) s += "-";
        long myestimate = StringSizeEstimator.estimatedSizeOf(s);
        long jammestimate = meter.measureDeep(s);
        System.out.println("String of size "+k+" estimates are us: "+myestimate+ " bytes jamm: "+jammestimate+" bytes");
        assertTrue(jammestimate <= myestimate);
        assertTrue(2 * jammestimate > myestimate);
      }
      System.out.println("All our string memory usage estimation are within a factor of two of jamm's and never lower.");

    }

    @Test
    public void stringSizeEstimator() {
      for(int k = 0; k < 10; ++k) {
        oneRoundOfStringSizeEstimation();
      }
    }

    @Test
    public void displayTest()  throws Exception {
        ExternalSort.main(new String[]{}); // check that it does not crash
    }


    @Test
    public void mainTest() throws Exception {
        ExternalSort.main(new String[]{"-h"}); // check that it does not crash
        ExternalSort.main(new String[]{""});// check that it does not crash
        ExternalSort.main(new String[]{"-v"}); // check that it does not crash
        File f1 = File.createTempFile("tmp", "unit");
        File f2 = File.createTempFile("tmp", "unit");
        f1.deleteOnExit();
        f2.deleteOnExit();
        writeStringToFile(f1, "oh");
        ExternalSort.main(new String[]{"-v","-d","-t","5000","-c","ascii","-z","-H","1","-s",".",f1.toString(),f2.toString()});
    }

    @Test
    public void testEmptyFiles() throws Exception {
        File f1 = File.createTempFile("tmp", "unit");
        File f2 = File.createTempFile("tmp", "unit");
        f1.deleteOnExit();
        f2.deleteOnExit();
        ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(f1),f2);
        if (f2.length() != 0) throw new RuntimeException("empty files should end up emtpy");
    }

    @Test
    public void testMergeSortedFiles() throws Exception {
        String line;

        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
        File out = File.createTempFile("test_results", ".tmp", null);
        out.deleteOnExit();
        ExternalSort.mergeSortedFiles(this.fileList, out, cmp,
                                      Charset.defaultCharset(), false);

        List<String> result = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(out))) {
            while ((line = bf.readLine()) != null) {
                result.add(line);
            }
        }
        assertArrayEquals(Arrays.toString(result.toArray()), EXPECTED_MERGE_RESULTS,
                          result.toArray());
    }

    @Test
    public void testMergeSortedFiles_Distinct() throws Exception {
        String line;


        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
        File out = File.createTempFile("test_results", ".tmp", null);
        out.deleteOnExit();
        ExternalSort.mergeSortedFiles(this.fileList, out, cmp,
                                      Charset.defaultCharset(), true);

        List<String> result = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(out))) {
            while ((line = bf.readLine()) != null) {
                result.add(line);
            }
        }
        assertArrayEquals(Arrays.toString(result.toArray()), EXPECTED_MERGE_DISTINCT_RESULTS,
                          result.toArray());
    }

    @Test
    public void testMergeSortedFiles_Append() throws Exception {
        String line;

        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2)
            {
                return o1.compareTo(o2);
            }
        };

        File out = File.createTempFile("test_results", ".tmp", null);
        out.deleteOnExit();
        writeStringToFile(out, "HEADER, HEADER\n");

        ExternalSort.mergeSortedFiles(this.fileList, out, cmp, Charset.defaultCharset(), true, true, false);

        List<String> result = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(out))) {
            while ((line = bf.readLine()) != null) {
                result.add(line);
            }
        }
        assertArrayEquals(Arrays.toString(result.toArray()), EXPECTED_HEADER_RESULTS, result.toArray());
    }

    @Test
    public void testSortAndSave() throws Exception {
        File f;
        String line;

        List<String> sample = Arrays.asList(SAMPLE);
        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };
        f = ExternalSort.sortAndSave(sample, cmp, Charset.defaultCharset(),
                                     null, false, false, true);
        assertNotNull(f);
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
        List<String> result = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(f))) {
            while ((line = bf.readLine()) != null) {
                result.add(line);
            }
        }
        assertArrayEquals(Arrays.toString(result.toArray()), EXPECTED_SORT_RESULTS,
                          result.toArray());
    }

    @Test
    public void testSortAndSave_Distinct() throws Exception {
        File f;
        String line;

        BufferedReader bf;
        List<String> sample = Arrays.asList(SAMPLE);
        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };

        f = ExternalSort.sortAndSave(sample, cmp, Charset.defaultCharset(),
                                     null, true, false, true);
        assertNotNull(f);
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
        bf = new BufferedReader(new FileReader(f));

        List<String> result = new ArrayList<>();
        while ((line = bf.readLine()) != null) {
            result.add(line);
        }
        bf.close();
        assertArrayEquals(Arrays.toString(result.toArray()),
                          EXPECTED_DISTINCT_RESULTS, result.toArray());
    }

    @Test
    public void testSortInBatch() throws Exception {
        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        };

        List<File> listOfFiles = ExternalSort.sortInBatch(this.csvFile, cmp, ExternalSort.DEFAULTMAXTEMPFILES, Charset.defaultCharset(), null, false, 1, false, true);
        assertEquals(1, listOfFiles.size());

        ArrayList<String> result = readLines(listOfFiles.get(0));
        assertArrayEquals(Arrays.toString(result.toArray()),EXPECTED_MERGE_DISTINCT_RESULTS, result.toArray());
    }

    /**
     * Sample case to sort csv file.
     * @throws Exception
     *
     */
    @Test
    public void testCSVSorting() throws Exception {
        testCSVSortingWithParams(false);
        testCSVSortingWithParams(true);
    }

    /**
     * Sample case to sort csv file.
     * @param usegzip use compression for temporary files
     * @throws Exception
     *
     */
    public void testCSVSortingWithParams(boolean usegzip) throws Exception {

        File out = File.createTempFile("test_results", ".tmp", null);
        out.deleteOnExit();
        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2)
            {
                return o1.compareTo(o2);
            }
        };

        String head;
        try ( // read header
                FileReader fr = new FileReader(this.csvFile)) {
            try (Scanner scan = new Scanner(fr)) {
                head = scan.nextLine();
            }
        }
        // write to the file
        writeStringToFile(out, head+"\n");

        // omit the first line, which is the header..
        List<File> listOfFiles = ExternalSort.sortInBatch(this.csvFile, cmp, ExternalSort.DEFAULTMAXTEMPFILES, Charset.defaultCharset(), null, false, 1, usegzip, true);

        // now merge with append
        ExternalSort.mergeSortedFiles(listOfFiles, out, cmp, Charset.defaultCharset(), false, true, usegzip);

        ArrayList<String> result = readLines(out);

        assertEquals(12, result.size());
        assertArrayEquals(Arrays.toString(result.toArray()),EXPECTED_HEADER_RESULTS, result.toArray());

    }

    public static ArrayList<String> readLines(File f) throws IOException {
        ArrayList<String> answer;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            answer = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                answer.add(line);
            }
        }
        return answer;
    }

    public static void writeStringToFile(File f, String s) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(s.getBytes());
        }
    }

}
