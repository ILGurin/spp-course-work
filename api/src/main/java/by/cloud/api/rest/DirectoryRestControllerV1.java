package by.cloud.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import by.cloud.api.service.DirectoryService;
import by.cloud.directory.dto.DirectoryDto;
import by.cloud.directory.dto.DirectoryPageDto;
import by.cloud.directory.dto.DirectoryWriteDto;
import by.cloud.directory.dto.DirectoryWriteResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/directories")
@CrossOrigin
public class DirectoryRestControllerV1 {

    private final DirectoryService directoryService;

    @PostMapping
    public Mono<ResponseEntity<DirectoryWriteResponseDto>> createDirectory(
            @Valid @RequestBody Mono<DirectoryWriteDto> body) {
        log.info("Received createDirectory request");
        return body.flatMap(directoryService::createDirectory)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Error in createDirectory endpoint", error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientRequestException ||
                        error.getCause() instanceof java.net.ConnectException) {
                        log.error("File-service is not available. Please ensure file-service is running on port 8093");
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping("/initialize/{userId}")
    public Mono<ResponseEntity<DirectoryWriteResponseDto>> createBaseDirectory(
            @PathVariable UUID userId) {
        log.info("Received createBaseDirectory request for userId=[{}]", userId);
        return directoryService.createBaseDirectory(userId)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Error in createBaseDirectory endpoint for userId=[{}]", userId, error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientRequestException ||
                        error.getCause() instanceof java.net.ConnectException) {
                        log.error("File-service is not available. Please ensure file-service is running on port 8093");
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping
    public Mono<ResponseEntity<DirectoryPageDto>> findAllDirectories(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "parentId", required = false) UUID parentId) {
        log.info("Received findAllDirectories request for userId=[{}], parentId=[{}]", userId, parentId);
        return directoryService.findAllDirectories(userId, parentId)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in findAllDirectories endpoint for userId=[{}], parentId=[{}]",
                            userId, parentId, error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientRequestException ||
                        error.getCause() instanceof java.net.ConnectException) {
                        log.error("File-service is not available. Please ensure file-service is running on port 8093");
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DirectoryDto>> findDirectoryById(@PathVariable UUID id) {
        return directoryService.findDirectoryById(id)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<DirectoryWriteResponseDto>> updateDirectory(
            @PathVariable UUID id,
            @Valid @RequestBody Mono<DirectoryWriteDto> body) {
        return body.flatMap(request -> directoryService.updateDirectory(id, request))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteDirectory(@PathVariable UUID id) {
        return directoryService.deleteDirectory(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}

