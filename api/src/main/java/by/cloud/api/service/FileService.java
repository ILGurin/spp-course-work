package by.cloud.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import by.cloud.file.api.FileApiClient;
import by.cloud.file.dto.FileDeleteResponseDto;
import by.cloud.file.dto.FileDto;
import by.cloud.file.dto.FilePageDto;
import by.cloud.file.dto.FileUploadResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    // Класс "оборачивает" обращения к file-service через feign клиент - FileApiClient

    private final FileApiClient fileApiClient;

    @WithSpan("fileService.uploadFiles")
    public Mono<FileUploadResponseDto> uploadFiles(List<MultipartFile> files, UUID userId, UUID directoryId) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Calling file-service uploadFiles with userId=[{}], directoryId=[{}], files count=[{}]", 
                                userId, directoryId, files != null ? files.size() : 0);
                        ResponseEntity<FileUploadResponseDto> response = fileApiClient.uploadFiles(userId, files, directoryId);
                        FileUploadResponseDto body = response.getBody();
                        return body;
                    } catch (Exception e) {
                        log.error("Error calling file-service uploadFiles", e);
                        throw e;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> log.info("Files uploaded for userId=[{}], directoryId=[{}], count=[{}]", 
                        userId, directoryId, response != null && response.getFiles() != null ? response.getFiles().size() : 0))
                .doOnError(error -> log.error("Error in uploadFiles for userId=[{}]", userId, error));
    }

    @WithSpan("fileService.findAllByUserId")
    public Mono<FilePageDto> findAllByUserId(UUID userId, UUID directoryId, Integer limit, Integer offset) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Calling file-service findAllByUserId with userId=[{}], directoryId=[{}], limit=[{}], offset=[{}]", 
                                userId, directoryId, limit, offset);
                        return fileApiClient.findAllByUserId(userId, directoryId, limit, offset);
                    } catch (Exception e) {
                        log.error("Error calling file-service findAllByUserId", e);
                        throw e;
                    }
                })
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(page -> log.info("Found [{}] files for userId=[{}], directoryId=[{}]", 
                        page.getTotal(), userId, directoryId))
                .doOnError(error -> log.error("Error in findAllByUserId for userId=[{}]", userId, error));
    }

    @WithSpan("fileService.findById")
    public Mono<FileDto> findById(UUID id) {
        return Mono.fromCallable(() -> fileApiClient.findById(id))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(file -> log.info("File found with id=[{}]", id));
    }

    @WithSpan("fileService.delete")
    public Mono<FileDeleteResponseDto> delete(UUID id) {
        return Mono.fromCallable(() -> fileApiClient.delete(id))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> log.info("File deleted with id=[{}]", id));
    }

    @WithSpan("fileService.downloadFile")
    public Mono<Resource> downloadFile(UUID id) {
        return Mono.fromCallable(() -> fileApiClient.downloadFile(id))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(resource -> log.info("File downloaded with id=[{}]", id));
    }
}
