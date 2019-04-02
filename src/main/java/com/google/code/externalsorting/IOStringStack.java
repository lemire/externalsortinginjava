package com.google.code.externalsorting;

import java.io.IOException;

/**
 * General interface to abstract away BinaryFileBuffer 
 * so that users of the library can roll their own.
 */
public interface IOStringStack {
    public void close() throws IOException;

    public boolean empty();

    public String peek();

    public String pop() throws IOException;

}