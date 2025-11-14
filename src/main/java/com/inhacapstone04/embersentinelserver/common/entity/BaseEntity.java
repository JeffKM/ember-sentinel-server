package com.inhacapstone04.embersentinelserver.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BaseEntity extends CreatedAtEntity {

    @LastModifiedDate // 엔티티 수정 시 자동으로 현재 시간이 주입됩니다.
    @Column(name = "modified_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE") // DB 컬럼 타입을 명시
    private OffsetDateTime modifiedAt;

}
