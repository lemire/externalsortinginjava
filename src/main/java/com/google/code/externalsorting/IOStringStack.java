package com.google.code.externalsorting;

import java.io.IOException;

/**
 * General interface to abstract away BinaryFileBuffer 
 * so that users of the library can roll their own.
 */
public interface IOStringStack {
    /**
     * Closes the underlying resource.
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException;

    /**
     * Checks if the stack is empty.
     * @return true if empty, false otherwise
     */
    public boolean empty();

    /**
     * Returns the next element without removing it.
     * @return the next element as a String
     */
    public String peek();

    /**
     * Removes and returns the next element.
     * @return the next element as a String
     * @throws IOException if an I/O error occurs
     */
    public String pop() throws IOException;

}