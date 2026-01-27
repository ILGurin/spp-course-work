package by.cloud.file_service.rest;

import by.cloud.directory.api.DirectoryApi;
import by.cloud.directory.dto.*;
import by.cloud.file_service.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FolderRestControllerV1 implements DirectoryApi {
    private final DirectoryService directoryService;

    @Override
    public ResponseEntity<DirectoryWriteResponseDto> createBaseDirectory(UUID userId) {
        var response = directoryService.createBaseDirectory(userId);
        return ResponseEntity.created(null).body(response);
    }

    @Override
    public ResponseEntity<DirectoryWriteResponseDto> createDirectory(DirectoryWriteDto directoryWriteDto) {
        var response = directoryService.createDirectory(directoryWriteDto);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteDirectory(UUID id) {
        directoryService.deleteDirectory(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<DirectoryPageDto> findAllDirectories(UUID userId, UUID parentId) {
        var pageDto = directoryService.findAllDirectories(userId, parentId);
        return ResponseEntity.ok(pageDto);
    }

    @Override
    public ResponseEntity<DirectoryDto> findDirectoryById(UUID id) {
        var directoryDto = directoryService.findById(id);
        return ResponseEntity.ok(directoryDto);
    }

    @Override
    public ResponseEntity<DirectoryWriteResponseDto> updateDirectory(UUID id, DirectoryWriteDto directoryWriteDto) {
        var response = directoryService.updateDirectory(id, directoryWriteDto);
        return ResponseEntity.ok(response);
    }
}
