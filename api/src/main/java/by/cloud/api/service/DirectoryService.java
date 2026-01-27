package by.cloud.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import by.cloud.directory.api.DirectoryApiClient;
import by.cloud.directory.dto.DirectoryDto;
import by.cloud.directory.dto.DirectoryPageDto;
import by.cloud.directory.dto.DirectoryWriteDto;
import by.cloud.directory.dto.DirectoryWriteResponseDto;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {
    // Класс "оборачивает" обращения к file-service через feign клиент - DirectoryApiClient

    private final DirectoryApiClient directoryApiClient;

    @WithSpan("directoryService.createDirectory")
    public Mono<DirectoryWriteResponseDto> createDirectory(DirectoryWriteDto request) {
        return Mono.fromCallable(() -> directoryApiClient.createDirectory(request))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> log.info("Directory created with id=[{}]", response.getId()));
    }

    @WithSpan("directoryService.createBaseDirectory")
    public Mono<DirectoryWriteResponseDto> createBaseDirectory(UUID userId) {
        return Mono.fromCallable(() -> directoryApiClient.createBaseDirectory(userId))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> log.info("Base directory created for userId=[{}] with id=[{}]", 
                        userId, response.getId()));
    }

    @WithSpan("directoryService.findAllDirectories")
    public Mono<DirectoryPageDto> findAllDirectories(UUID userId, UUID parentId) {
        return Mono.fromCallable(() -> {
                    try {
                        log.info("Calling file-service findAllDirectories with userId=[{}], parentId=[{}]", 
                                userId, parentId);
                        return directoryApiClient.findAllDirectories(userId, parentId);
                    } catch (Exception e) {
                        log.error("Error calling file-service findAllDirectories", e);
                        throw e;
                    }
                })
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(page -> log.info("Found directories for userId=[{}], parentId=[{}]", 
                        userId, parentId))
                .doOnError(error -> log.error("Error in findAllDirectories for userId=[{}]", userId, error));
    }

    @WithSpan("directoryService.findDirectoryById")
    public Mono<DirectoryDto> findDirectoryById(UUID id) {
        return Mono.fromCallable(() -> directoryApiClient.findDirectoryById(id))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(directory -> log.info("Directory found with id=[{}]", id));
    }

    @WithSpan("directoryService.updateDirectory")
    public Mono<DirectoryWriteResponseDto> updateDirectory(UUID id, DirectoryWriteDto request) {
        return Mono.fromCallable(() -> directoryApiClient.updateDirectory(id, request))
                .mapNotNull(HttpEntity::getBody)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(response -> log.info("Directory updated with id=[{}]", id));
    }

    @WithSpan("directoryService.deleteDirectory")
    public Mono<Void> deleteDirectory(UUID id) {
        return Mono.fromRunnable(() -> directoryApiClient.deleteDirectory(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v -> log.info("Directory deleted with id=[{}]", id));
    }
}

