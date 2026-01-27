package by.cloud.file_service.repository;

import by.cloud.file_service.entity.Directory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import java.util.List;

public interface DirectoryRepository extends JpaRepository<Directory, UUID> {
    @Query("FROM Directory d WHERE d.parentId IS NULL AND d.userId = :userId AND d.active = true ORDER BY d.created ASC")
    List<Directory> findBaseDirectories(@Param("userId") UUID userId);
    
    default Optional<Directory> findBaseDirectory(UUID userId) {
        var directories = findBaseDirectories(userId);
        if (directories.isEmpty()) {
            return Optional.empty();
        }
        // Если найдено несколько корневых директорий, возвращаем первую (самую старую)
        // Это может произойти из-за race condition или ошибок в данных
        if (directories.size() > 1) {
            // Логируем предупреждение, но возвращаем первую директорию
            // В идеале нужно исправить данные в БД, чтобы была только одна корневая директория
            System.err.println("WARNING: Found " + directories.size() + " base directories for user " + userId + ". Using the oldest one.");
        }
        return Optional.of(directories.get(0));
    }

    @Query("FROM Directory d WHERE d.active = true AND d.userId = :userId AND d.parentId IS NOT NULL AND (:parentId IS NULL OR d.parentId = :parentId)")
    List<Directory> findByUserIdAndParentId(@Param("userId") UUID userId, @Param("parentId") UUID parentId);
}
