package by.cloud.file_service.repository;

import by.cloud.file_service.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    @Query("FROM File f WHERE f.active = true AND f.userId = :userId AND f.directory.id = :folderId")
    List<File> findByUserIdAndFolderId(@Param("userId") UUID userId, @Param("folderId") UUID folderId);
}
