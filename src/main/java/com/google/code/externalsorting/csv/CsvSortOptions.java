package com.google.code.externalsorting.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.nio.charset.Charset;
import java.util.Comparator;

/**
 * Parameters for csv sorting
 */
public class CsvSortOptions {
    private final Comparator<CSVRecord> comparator;
    private final int maxTmpFiles;
    private final long maxMemory;
    private final Charset charset;

    private final boolean distinct;
    private final int numHeader; //number of header row in input file
    private final boolean skipHeader; //print header or not to output file
    private final CSVFormat format;

    /**
     * Gets the comparator used for sorting CSV records.
     * @return the comparator
     */
    public Comparator<CSVRecord> getComparator() {
        return comparator;
    }
    /**
     * Gets the maximum number of temporary files.
     * @return the max number of temp files
     */
    public int getMaxTmpFiles() {
        return maxTmpFiles;
    }
    /**
     * Gets the maximum memory to use (in bytes).
     * @return the max memory
     */
    public long getMaxMemory() {
        return maxMemory;
    }
    /**
     * Gets the charset used for reading/writing CSV files.
     * @return the charset
     */
    public Charset getCharset() {
        return charset;
    }
    /**
     * Indicates whether distinct records should be used.
     * @return true if distinct, false otherwise
     */
    public boolean isDistinct() {
        return distinct;
    }
    /**
     * Gets the number of header rows in the input file.
     * @return the number of header rows
     */
    public int getNumHeader() {
        return numHeader;
    }
    /**
     * Indicates whether the header should be skipped in the output file.
     * @return true if header is skipped, false otherwise
     */
    public boolean isSkipHeader() {
        return skipHeader;
    }
    /**
     * Gets the CSV format used for parsing and writing.
     * @return the CSVFormat
     */
    public CSVFormat getFormat() {
        return format;
    }

    /**
     * Builder class for constructing CsvSortOptions with custom parameters.
     */
    public static class Builder {
        //mandatory params
        private final Comparator<CSVRecord> cmp;
        private final int maxTmpFiles;
        private final long maxMemory;

        //optional params with default values
        private Charset cs = Charset.defaultCharset();
        private boolean distinct = false;
        private int numHeader = 0;
        private boolean skipHeader = true;
        private CSVFormat format = CSVFormat.DEFAULT;

        /**
         * Constructs a Builder for CsvSortOptions.
         * @param cmp the comparator for sorting
         * @param maxTmpFiles the max number of temp files
         * @param maxMemory the max memory to use
         */
        public Builder(Comparator<CSVRecord> cmp, int maxTmpFiles, long maxMemory) {
            this.cmp = cmp;
            this.maxTmpFiles = maxTmpFiles;
            this.maxMemory = maxMemory;
        }
        /**
         * Sets the charset for CSV operations.
         * @param value the charset
         * @return this builder
         */
        public Builder charset(Charset value){
            cs = value;
            return this;
        }
        /**
         * Sets whether to use distinct records.
         * @param value true for distinct
         * @return this builder
         */
        public Builder distinct(boolean value){
            distinct = value;
            return this;
        }
        /**
         * Sets the number of header rows.
         * @param value the number of header rows
         * @return this builder
         */
        public Builder numHeader(int value){
            numHeader = value;
            return this;
        }
        /**
         * Sets whether to skip the header in output.
         * @param value true to skip header
         * @return this builder
         */
        public Builder skipHeader(boolean value){
            skipHeader = value;
            return this;
        }
        /**
         * Sets the CSV format.
         * @param value the CSVFormat
         * @return this builder
         */
        public Builder format(CSVFormat value){
            format = value;
            return this;
        }
        /**
         * Builds the CsvSortOptions instance.
         * @return a new CsvSortOptions
         */
        public CsvSortOptions build(){
            return new CsvSortOptions(this);
        }
    }

    private CsvSortOptions(Builder builder){
        this.comparator = builder.cmp;
        this.maxTmpFiles = builder.maxTmpFiles;
        this.maxMemory = builder.maxMemory;
        this.charset = builder.cs;
        this.distinct = builder.distinct;
        this.numHeader = builder.numHeader;
        this.skipHeader = builder.skipHeader;
        this.format = builder.format;
    }

}
