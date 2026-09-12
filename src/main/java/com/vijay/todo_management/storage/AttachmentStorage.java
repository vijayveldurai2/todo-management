package com.vijay.todo_management.storage;
import java.io.*;
import java.util.List;
public interface AttachmentStorage {
    long write(String key, InputStream input, long limit) throws IOException;
    InputStream open(String key) throws IOException;
    void delete(String key) throws IOException;
    List<String> oldKeys() throws IOException;
}

