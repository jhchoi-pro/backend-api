package com.fullstack.backend_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 👈 엔티티들이 상속받아 필드를 사용할 수 있도록 지정
@EntityListeners(AuditingEntityListener.class) // 👈 JPA Auditing 기능을 활성화
public abstract class BaseTimeEntity {

    @CreatedDate // 👈 엔티티 생성 시 시간이 자동 저장됨
    @Column(updatable = false) // 생성 시간은 업데이트되지 않도록 설정
    private LocalDateTime createdAt;

    @LastModifiedDate // 👈 엔티티 수정 시 시간이 자동 업데이트됨
    private LocalDateTime modifiedAt;
}