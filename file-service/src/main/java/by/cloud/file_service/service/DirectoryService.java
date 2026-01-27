package by.cloud.file_service.service;

import by.cloud.directory.dto.DirectoryDto;
import by.cloud.directory.dto.DirectoryPageDto;
import by.cloud.directory.dto.DirectoryWriteDto;
import by.cloud.directory.dto.DirectoryWriteResponseDto;
import by.cloud.file_service.entity.Directory;
import by.cloud.file_service.exception.DirectoryConflictException;
import by.cloud.file_service.exception.FileException;
import by.cloud.file_service.mapper.DirectoryMapper;
import by.cloud.file_service.repository.DirectoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Setter
@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final DirectoryMapper directoryMapper;
    private final DirectoryRepository directoryRepository;

    @Transactional
    public DirectoryWriteResponseDto createBaseDirectory(UUID userId) {
        var existingDirectories = directoryRepository.findBaseDirectories(userId);
        if (!existingDirectories.isEmpty()) {
            throw new DirectoryConflictException("Base directory already exists for user [%s]", userId);
        }
        
        var baseDirectory = directoryMapper.toBaseDirectory(userId);
        directoryRepository.save(baseDirectory);
        
        log.info("IN - createBaseDirectory: base directory successfully created for user [{}]", userId.toString());
        return new DirectoryWriteResponseDto(baseDirectory.getId());
    }

    @Transactional
    public DirectoryWriteResponseDto createDirectory(DirectoryWriteDto dto) {
        // Если parentId не указан, находим корневую директорию пользователя
        // и устанавливаем её как родительскую
        if (dto.getParentId() == null) {
            var baseDirectory = directoryRepository.findBaseDirectory(dto.getUserId())
                    .orElseGet(() -> {
                        // Если корневой директории нет, создаем её автоматически
                        log.info("Base directory not found for user [{}], creating it automatically", dto.getUserId());
                        var response = createBaseDirectory(dto.getUserId());
                        return directoryRepository.findById(response.getId())
                                .orElseThrow(() -> new FileException("Failed to create base directory for user %s", dto.getUserId()));
                    });
            // Устанавливаем корневую директорию как родительскую
            dto.setParentId(baseDirectory.getId());
        }
        
        var directory = directoryMapper.to(dto);
        directoryRepository.save(directory);
        
        log.info("IN - createDirectory: directory successfully created with id [{}], parentId=[{}]", 
                directory.getId(), directory.getParentId());
        return new DirectoryWriteResponseDto(directory.getId());
    }

    public DirectoryDto findById(UUID id) {
        var directory = directoryRepository.findById(id)
                .orElseThrow(() -> new FileException("Directory not found by id=[%s]", id));
        log.info("IN - findById: directory with id = [{}] successfully found", id);
        var directoryDto = directoryMapper.from(directory);
        return directoryDto;
    }

    public DirectoryPageDto findAllDirectories(UUID userId, UUID parentId) {
        // Если parentId не указан, находим корневую директорию пользователя
        // и используем её ID для поиска дочерних директорий
        UUID actualParentId = parentId;
        if (actualParentId == null) {
            var baseDirectory = directoryRepository.findBaseDirectory(userId)
                    .orElse(null);
            if (baseDirectory != null) {
                actualParentId = baseDirectory.getId();
            } else {
                // Если корневой директории нет, возвращаем пустой список
                var emptyPageDto = new DirectoryPageDto();
                emptyPageDto.setItems(new ArrayList<>());
                log.info("IN - findAllDirectories: base directory not found for userId=[{}], returning empty list", userId);
                return emptyPageDto;
            }
        }
        
        var directories = directoryRepository.findByUserIdAndParentId(userId, actualParentId);
        var directoryDtos = directories.stream()
                .map(directoryMapper::from)
                .toList();
        var pageDto = new DirectoryPageDto();
        pageDto.setItems(directoryDtos);
        
        log.info("IN - findAllDirectories: found [{}] directories for userId=[{}], parentId=[{}], actualParentId=[{}]", 
                directoryDtos.size(), userId, parentId, actualParentId);
        return pageDto;
    }

    @Transactional
    public DirectoryWriteResponseDto updateDirectory(UUID id, DirectoryWriteDto dto) {
        var directory = directoryRepository.findById(id)
                .orElseThrow(() -> new FileException("Directory not found by id=[%s]", id));
        directoryMapper.update(directory, dto);
        directoryRepository.save(directory);
        
        log.info("IN - updateDirectory: directory with id = [{}] successfully updated", id);
        return new DirectoryWriteResponseDto(directory.getId());
    }

    @Transactional
    public void deleteDirectory(UUID id) {
        var directory = directoryRepository.findById(id)
                .orElseThrow(() -> new FileException("Directory not found by id=[%s]", id));
        directory.setActive(false);
        directoryRepository.save(directory);
        
        log.info("IN - deleteDirectory: directory with id = [{}] successfully deleted", id);
    }
}
