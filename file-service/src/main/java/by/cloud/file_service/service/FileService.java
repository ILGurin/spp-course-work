package by.cloud.file_service.service;

import by.cloud.file.dto.FileDeleteResponseDto;
import by.cloud.file.dto.FileDto;
import by.cloud.file.dto.FilePageDto;
import by.cloud.file.dto.FileUploadResponseDto;
import by.cloud.file_service.config.MinioConfig;
import by.cloud.file_service.entity.Directory;
import by.cloud.file_service.entity.File;
import by.cloud.file_service.exception.DirectoryConflictException;
import by.cloud.file_service.exception.FileException;
import by.cloud.file_service.mapper.FileMapper;
import by.cloud.file_service.repository.DirectoryRepository;
import by.cloud.file_service.repository.FileRepository;
import by.cloud.file_service.util.DateTimeUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final DirectoryService directoryService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileMapper fileMapper;
    private final DateTimeUtil dateTimeUtil;

    @Transactional
    public FileUploadResponseDto upload(List<MultipartFile> files, UUID userId, UUID folderId) {
        Directory directory;
        if (folderId == null) {
            // Если directoryId не указан, используем корневую директорию пользователя
            directory = directoryRepository.findBaseDirectory(userId)
                    .orElseGet(() -> {
                        // Автоматически создаем базовую директорию, если её нет
                        log.info("Base directory not found for user [{}], creating it automatically", userId);
                        try {
                            var response = directoryService.createBaseDirectory(userId);
                            return directoryRepository.findById(response.getId())
                                    .orElseThrow(() -> new FileException("Failed to create base directory for user %s", userId));
                        } catch (DirectoryConflictException e) {
                            // Если директория была создана другим потоком, пытаемся найти её снова
                            log.warn("Base directory was created concurrently for user [{}], retrying to find it", userId);
                            return directoryRepository.findBaseDirectory(userId)
                                    .orElseThrow(() -> new FileException("Base directory not found for user %s after creation attempt", userId));
                        }
                    });
        } else {
            directory = directoryRepository.findById(folderId)
                    .orElseThrow(() -> new FileException("Directory with id %s not found", folderId));
        }

        // Проверяем, что директория принадлежит пользователю
        if (!directory.getUserId().equals(userId)) {
            throw new FileException("Directory does not belong to user %s", userId);
        }


        List<FileDto> uploadedFiles = new ArrayList<>();

        for (MultipartFile multipartFile : files) {
            if (multipartFile.isEmpty()) {
                log.warn("Skipping empty file: {}", multipartFile.getOriginalFilename());
                continue;
            }

            try {
                File file = uploadFile(multipartFile, userId, directory);
                FileDto fileDto = fileMapper.toDto(file);
                // Генерируем URL для скачивания
                fileDto.setDownloadUrl(generateDownloadUrl(file.getId()));
                uploadedFiles.add(fileDto);
                
                log.info("Successfully uploaded file: {} with id: {}", multipartFile.getOriginalFilename(), file.getId());
            } catch (Exception e) {
                log.error("Error uploading file: {}", multipartFile.getOriginalFilename(), e);
                throw new FileException("Failed to upload file %s: %s", multipartFile.getOriginalFilename(), e.getMessage());
            }
        }

        FileUploadResponseDto response = new FileUploadResponseDto();
        response.setFiles(uploadedFiles);
        return response;
    }

    private File uploadFile(MultipartFile multipartFile, UUID userId, Directory directory) throws Exception {
        // Генерируем уникальное имя объекта в MinIO
        String objectName = UUID.randomUUID().toString();
        String fileName = multipartFile.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "unnamed_file";
        }

        // Проверяем и создаем bucket, если его нет
        ensureBucketExists();
        
        // Загружаем файл в MinIO
        try (InputStream inputStream = multipartFile.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(inputStream, multipartFile.getSize(), -1)
                            .contentType(multipartFile.getContentType())
                            .build()
            );
            String presignedObjectUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .expiry(24 * 60 * 60)
                            .build()
            );
            System.out.println(presignedObjectUrl);
        } catch (MinioException e) {
            log.error("MinIO error while uploading file: {}", fileName, e);
            throw new FileException("Failed to upload file to MinIO: %s", e.getMessage());
        }

        // Сохраняем метаданные в БД
        File file = new File();
        file.setUserId(userId);
        file.setDirectory(directory);
        file.setFileName(fileName);
        file.setObjectName(objectName);
        file.setFileSize((int) multipartFile.getSize());
        file.setMimeType(multipartFile.getContentType() != null ? multipartFile.getContentType() : "application/octet-stream");
        file.setActive(true);
        file.setCreated(dateTimeUtil.now());
        file.setUpdated(dateTimeUtil.now());

        return fileRepository.save(file);
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .build());
            
            if (!found) {
                log.warn("Bucket [{}] does not exist, creating it...", minioConfig.getBucket());
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .build());
                log.info("Bucket [{}] successfully created", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.error("Error ensuring bucket [{}] exists: {}", minioConfig.getBucket(), e.getMessage());
            throw new FileException("Failed to ensure MinIO bucket exists: %s", e.getMessage());
        }
    }

    private String generateDownloadUrl(UUID fileId) {
        // Генерируем URL для скачивания файла
        // В реальном приложении это может быть presigned URL от MinIO
        return String.format("/v1/files/download/%s", fileId);
    }

    public FileDto findById(UUID id) {
        var file = fileRepository.findById(id)
                .orElseThrow(() -> new FileException("File not found by id=[%s]", id));
        if (!file.getActive()) {
            throw new FileException("File not found by id=[%s]", id);
        }
        log.info("IN - findById: file with id = [{}] successfully found", id);
        var fileDto = fileMapper.toDto(file);
        fileDto.setDownloadUrl(generateDownloadUrl(file.getId()));
        return fileDto;
    }

    public FilePageDto findAllByUserId(UUID userId, UUID folderId, Integer limit, Integer offset) {
        int actualLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;
        int actualOffset = (offset != null && offset >= 0) ? offset : 0;

        // Если folderId не указан, находим корневую директорию пользователя
        UUID actualFolderId = folderId;
        if (actualFolderId == null) {
            var baseDirectory = directoryRepository.findBaseDirectory(userId)
                    .orElse(null);
            if (baseDirectory == null) {
                // Если корневой директории нет, возвращаем пустой список
                var emptyPageDto = new FilePageDto();
                emptyPageDto.setItems(new ArrayList<>());
                emptyPageDto.setTotal(0);
                emptyPageDto.setLimit(actualLimit);
                emptyPageDto.setOffset(actualOffset);
                return emptyPageDto;
            }
            actualFolderId = baseDirectory.getId();
        }

        // Получаем файлы только из указанной директории (или корневой, если не указана)
        var allFiles = fileRepository.findByUserIdAndFolderId(userId, actualFolderId);
        
        // Убираем дубликаты по ID (на случай проблем с кэшем или транзакциями)
        var uniqueFiles = allFiles.stream()
                .collect(Collectors.toMap(
                    File::getId,
                    file -> file,
                    (existing, replacement) -> existing, // Оставляем первый найденный
                    LinkedHashMap::new // Сохраняем порядок
                ))
                .values();
        
        var total = uniqueFiles.size();

        var pagedFiles = uniqueFiles.stream()
                .skip(actualOffset)
                .limit(actualLimit)
                .map(file -> {
                    var fileDto = fileMapper.toDto(file);
                    fileDto.setDownloadUrl(generateDownloadUrl(file.getId()));
                    return fileDto;
                })
                .toList();

        var pageDto = new FilePageDto();
        pageDto.setItems(pagedFiles);
        pageDto.setTotal(total);
        pageDto.setLimit(actualLimit);
        pageDto.setOffset(actualOffset);

        log.info("IN - findAllByUserId: found [{}] files for userId=[{}], folderId=[{}]", 
                pagedFiles.size(), userId, folderId);
        return pageDto;
    }

    @Transactional
    public FileDeleteResponseDto delete(UUID id) {
        var file = fileRepository.findById(id)
                .orElseThrow(() -> new FileException("File not found by id=[%s]", id));
        if (!file.getActive()) {
            throw new FileException("File not found by id=[%s]", id);
        }
        file.setActive(false);
        fileRepository.save(file);
        
        log.info("IN - delete: file with id = [{}] successfully deleted", id);
        return new FileDeleteResponseDto(file.getId());
    }

    public Resource downloadFile(UUID id) {
        var file = fileRepository.findById(id)
                .orElseThrow(() -> new FileException("File not found by id=[%s]", id));
        if (!file.getActive()) {
            throw new FileException("File not found by id=[%s]", id);
        }

        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(file.getObjectName())
                            .build()
            );
            log.info("IN - downloadFile: file with id = [{}] successfully downloaded", id);
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            log.error("Error downloading file: {}", id, e);
            throw new FileException("Failed to download file: %s", e.getMessage());
        }
    }
}
