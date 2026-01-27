package by.cloud.api.rest;

import by.cloud.api.service.FileService;
import by.cloud.file.dto.FileDeleteResponseDto;
import by.cloud.file.dto.FileDto;
import by.cloud.file.dto.FilePageDto;
import by.cloud.file.dto.FileUploadResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/files")
@CrossOrigin
public class FileRestControllerV1 {

    private final FileService fileService;

    @PostMapping(consumes = "multipart/form-data")
    public Mono<ResponseEntity<FileUploadResponseDto>> uploadFiles(
            @RequestPart("files") Flux<Part> parts,
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "directoryId", required = false) UUID directoryId) {
        log.info("Received uploadFiles request for userId=[{}], directoryId=[{}]", userId, directoryId);
        
        return parts
                .filter(part -> part instanceof FilePart)
                .cast(FilePart.class)
                .flatMap(this::convertFilePartToMultipartFile)
                .collectList()
                .flatMap(files -> {
                    log.info("Converted [{}] files for upload", files.size());
                    return fileService.uploadFiles(files, userId, directoryId);
                })
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Error in uploadFiles endpoint for userId=[{}], directoryId=[{}]",
                            userId, directoryId, error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientRequestException ||
                        error.getCause() instanceof java.net.ConnectException) {
                        log.error("File-service is not available. Please ensure file-service is running on port 8093");
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
    
    private Mono<MultipartFile> convertFilePartToMultipartFile(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .map(bytes -> new MultipartFile() {
                    @Override
                    public String getName() {
                        return filePart.name();
                    }

                    @Override
                    public String getOriginalFilename() {
                        return filePart.filename();
                    }

                    @Override
                    public String getContentType() {
                        return filePart.headers().getFirst("Content-Type");
                    }

                    @Override
                    public boolean isEmpty() {
                        return bytes.length == 0;
                    }

                    @Override
                    public long getSize() {
                        return bytes.length;
                    }

                    @Override
                    public byte[] getBytes() throws IOException {
                        return bytes;
                    }

                    @Override
                    public java.io.InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(bytes);
                    }

                    @Override
                    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                        throw new UnsupportedOperationException("transferTo not supported");
                    }
                });
    }

    @GetMapping
    public Mono<ResponseEntity<FilePageDto>> findAllByUserId(
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "directoryId", required = false) UUID directoryId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset) {
        log.info("Received findAllByUserId request for userId=[{}], directoryId=[{}], limit=[{}], offset=[{}]",
                userId, directoryId, limit, offset);
        return fileService.findAllByUserId(userId, directoryId, limit, offset)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error in findAllByUserId endpoint for userId=[{}], directoryId=[{}]", 
                            userId, directoryId, error);
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientRequestException ||
                        error.getCause() instanceof java.net.ConnectException) {
                        log.error("File-service is not available. Please ensure file-service is running on port 8093");
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<FileDto>> findById(@PathVariable UUID id) {
        return fileService.findById(id)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<FileDeleteResponseDto>> delete(@PathVariable UUID id) {
        return fileService.delete(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/download/{id}")
    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable UUID id) {
        return fileService.findById(id)
                .flatMap(fileDto -> fileService.downloadFile(id)
                        .map(resource -> ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + fileDto.getFileName() + "\"")
                                .header("Content-Type", fileDto.getMimeType())
                                .body(resource)));
    }
}

