package by.cloud.file_service.rest;

import by.cloud.file.api.FileApi;
import by.cloud.file.dto.FileDeleteResponseDto;
import by.cloud.file.dto.FileDto;
import by.cloud.file.dto.FilePageDto;
import by.cloud.file.dto.FileUploadResponseDto;
import by.cloud.file_service.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileRestControllerV1 implements FileApi {
    private final FileService fileService;

    @Override
    public ResponseEntity<FileDeleteResponseDto> delete(UUID id) {
        var response = fileService.delete(id);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Resource> downloadFile(UUID id) {
        var fileDto = fileService.findById(id);
        var resource = fileService.downloadFile(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileDto.getFileName() + "\"")
                .header("Content-Type", fileDto.getMimeType())
                .body(resource);
    }

    @Override
    public ResponseEntity<FilePageDto> findAllByUserId(UUID userId, UUID directoryId, Integer limit, Integer offset) {
        var pageDto = fileService.findAllByUserId(userId, directoryId, limit, offset);
        return ResponseEntity.ok(pageDto);
    }

    @Override
    public ResponseEntity<FileDto> findById(UUID id) {
        var fileDto = fileService.findById(id);
        return ResponseEntity.ok(fileDto);
    }

    @Override
    public ResponseEntity<FileUploadResponseDto> uploadFiles(
            UUID userId,
            List<MultipartFile> files,
            UUID directoryId) {
        var response = fileService.upload(files, userId, directoryId);
        return ResponseEntity.status(201).body(response);
    }
}
