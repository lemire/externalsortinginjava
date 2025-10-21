package com.google.code.externalsorting.csv;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Buffer wrapper for CSVRecord iteration and management.
 * Handles reading and closing of CSVParser resources.
 */
public class CSVRecordBuffer {

	private Iterator<CSVRecord> iterator;

	private CSVParser parser;

	private CSVRecord cache;

	/**
	 * Constructs a CSVRecordBuffer wrapping the given CSVParser.
	 * @param parser the CSVParser to wrap
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if a class cannot be found
	 */
	public CSVRecordBuffer(CSVParser parser) throws IOException, ClassNotFoundException {
		this.iterator = parser.iterator();
		this.parser = parser;
		reload();
	}

	/**
	 * Closes the underlying CSVParser.
	 * @throws IOException if an I/O error occurs
	 */
	public void close() throws IOException {
		this.parser.close();
	}

	/**
	 * Checks if the buffer is empty.
	 * @return true if there are no more records to read
	 */
	public boolean empty() {
		return this.cache == null;
	}

	/**
	 * Returns the next CSVRecord in the buffer without removing it.
	 * @return the next CSVRecord, or null if empty
	 */
	public CSVRecord peek() {
		return this.cache;
	}

	/**
	 * Removes and returns the next CSVRecord in the buffer.
	 * @return the next CSVRecord
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if a class cannot be found
	 */
	public CSVRecord pop() throws IOException, ClassNotFoundException {
		CSVRecord answer = peek();// make a copy
		reload();
		return answer;
	}

	// Get the next in line
	private void reload() throws IOException, ClassNotFoundException {
		this.cache = this.iterator.hasNext() ? this.iterator.next() : null;
	}
}
