package by.cloud.file_service.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
@Getter
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        // Инициализируем bucket при старте приложения
        initializeBucket(client);
        
        return client;
    }

    private void initializeBucket(MinioClient client) {
        try {
            // Проверяем, существует ли bucket
            boolean found = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build());
            
            if (!found) {
                // Создаем bucket, если его нет
                log.info("Bucket [{}] does not exist, creating it...", bucket);
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
                log.info("Bucket [{}] successfully created", bucket);
            } else {
                log.info("Bucket [{}] already exists", bucket);
            }
        } catch (MinioException e) {
            log.error("Error checking/creating bucket [{}]: {}", bucket, e.getMessage());
            throw new RuntimeException("Failed to initialize MinIO bucket: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error while initializing bucket [{}]: {}", bucket, e.getMessage());
            throw new RuntimeException("Failed to initialize MinIO bucket: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while initializing bucket [{}]: {}", bucket, e.getMessage());
            throw new RuntimeException("Failed to initialize MinIO bucket: " + e.getMessage(), e);
        }
    }
}

