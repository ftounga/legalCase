package fr.ailegalcase.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final StorageProperties props;

    public S3StorageService(S3Client s3Client, StorageProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    @PostConstruct
    void initBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(props.getBucket()).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(props.getBucket()).build());
            log.info("Bucket '{}' created", props.getBucket());
        } catch (Exception e) {
            log.warn("Could not verify/create bucket '{}': {}. Storage may be unavailable.",
                    props.getBucket(), e.getMessage());
        }
    }

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long contentLength) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build(),
                RequestBody.fromInputStream(inputStream, contentLength)
        );
        return key;
    }
}
