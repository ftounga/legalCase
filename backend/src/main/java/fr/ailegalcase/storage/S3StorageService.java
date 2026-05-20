package fr.ailegalcase.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

import java.io.InputStream;
import java.util.Optional;

@Service
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties props;
    private final CloudFrontProperties cloudFrontProps;
    private final Optional<CloudFrontUrlSigner> cloudFrontSigner;

    public S3StorageService(S3Client s3Client,
                            S3Presigner presigner,
                            StorageProperties props,
                            CloudFrontProperties cloudFrontProps,
                            Optional<CloudFrontUrlSigner> cloudFrontSigner) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
        this.cloudFrontProps = cloudFrontProps;
        this.cloudFrontSigner = cloudFrontSigner;
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

    @Override
    public byte[] download(String key) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(props.getBucket()).key(key).build()
        ).asByteArray();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(props.getBucket()).key(key).build()
        );
    }

    @Override
    public String presignedDownloadUrl(String key, int expirationMinutes) {
        // SF-INFRA-08 : si CloudFront est configuré, génère une URL signée CDN ;
        // sinon fallback URL S3 présignée native (dev local, MinIO).
        if (cloudFrontProps.isEnabled() && cloudFrontSigner.isPresent()) {
            return cloudFrontSigner.get().signedUrl(key, expirationMinutes * 60L);
        }
        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(r -> r.bucket(props.getBucket()).key(key))
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
