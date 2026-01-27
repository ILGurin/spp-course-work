package by.cloud.file_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.UUID;

@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Setter
@Getter
@Entity
@Table(name = "files", schema = "file")
public class File extends BaseEntity {

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "directory_id", nullable = false)
    private Directory directory;

    @NotNull
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotNull
    @Size(max = 64)
    @Column(name = "object_name", nullable = false)
    private String objectName;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @NotNull
    @Size(max = 128)
    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;
}
