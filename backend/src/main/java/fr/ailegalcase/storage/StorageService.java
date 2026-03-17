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

    /**
     * Generate a presigned URL to download an object.
     *
     * @param key              the object key in the bucket
     * @param expirationMinutes how long the URL is valid
     * @return a presigned URL string
     */
    String presignedDownloadUrl(String key, int expirationMinutes);
}
