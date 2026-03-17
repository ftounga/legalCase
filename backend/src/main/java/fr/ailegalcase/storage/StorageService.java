package fr.ailegalcase.storage;

import java.io.InputStream;

public interface StorageService {

    /**
     * Upload a file to object storage.
     *
     * @param key           the object key (path) in the bucket
     * @param inputStream   file content
     * @param contentType   MIME type
     * @param contentLength file size in bytes
     * @return the storage key used
     */
    String upload(String key, InputStream inputStream, String contentType, long contentLength);
}
