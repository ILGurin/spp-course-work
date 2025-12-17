package by.cloud.person_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.validation.constraints.Size;

@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Setter
@Getter
@Entity
@Table(name = "users", schema = "person")
public class User extends BaseEntity {
    @Size(max = 1024)
    @Column(name = "email", nullable = false, unique = true, length = 1024)
    private String email;

    @Size(max = 64)
    @Column(name = "first_name", nullable = false, unique = true, length = 64)
    private String firstName;

    @Size(max = 64)
    @Column(name = "last_name", nullable = false, unique = true, length = 64)
    private String lastName;
}
