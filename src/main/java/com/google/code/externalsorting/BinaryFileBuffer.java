package com.google.code.externalsorting;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This is essentially a thin wrapper on top of a BufferedReader... which keeps
 * the last line in memory.
 *
 */
public final class BinaryFileBuffer implements IOStringStack {
    /**
     * Constructs a BinaryFileBuffer wrapping the given BufferedReader.
     * @param r the BufferedReader to wrap
     * @throws IOException if an I/O error occurs
     */
    public BinaryFileBuffer(BufferedReader r) throws IOException {
        this.fbr = r;
        reload();
    }
    /**
     * Closes the underlying BufferedReader.
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        this.fbr.close();
    }
    /**
     * Checks if the buffer is empty.
     * @return true if there are no more lines to read
     */
    public boolean empty() {
        return this.cache == null;
    }
    /**
     * Returns the next line in the buffer without removing it.
     * @return the next line as a String, or null if empty
     */
    public String peek() {
        return this.cache;
    }
    /**
     * Removes and returns the next line in the buffer.
     * @return the next line as a String
     * @throws IOException if an I/O error occurs
     */
    public String pop() throws IOException {
        String answer = peek().toString();// make a copy
        reload();
        return answer;
    }

    private void reload() throws IOException {
        this.cache = this.fbr.readLine();
    }

    private BufferedReader fbr;

    private String cache;

}