package by.cloud.person_service.entity;

import jakarta.persistence.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@RevisionEntity
@Table(name = "revinfo", schema = "person_history")
public class BaseEnversUtilEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    @Column(name = "rev")
    private long rev;

    @RevisionTimestamp
    @Column(name = "revtmstmp")
    private long revtmstmp;
}
