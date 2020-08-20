package com.google.code.externalsorting.csv;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CSVRecordBuffer {

	private Iterator<CSVRecord> iterator;

	private CSVParser parser;

	private CSVRecord cache;

	public CSVRecordBuffer(CSVParser parser) throws IOException, ClassNotFoundException {
		this.iterator = parser.iterator();
		this.parser = parser;
		reload();
	}

	public void close() throws IOException {
		this.parser.close();
	}

	public boolean empty() {
		return this.cache == null;
	}

	public CSVRecord peek() {
		return this.cache;
	}

	//
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
