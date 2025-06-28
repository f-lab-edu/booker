package com.bookerapp.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * 생성자 정보를 설정합니다.
     * 주로 인증된 사용자 정보를 기반으로 설정됩니다.
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 수정자 정보를 설정합니다.
     * 주로 인증된 사용자 정보를 기반으로 설정됩니다.
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 소프트 삭제를 수행합니다.
     * 실제 데이터를 삭제하지 않고 삭제 플래그만 변경합니다.
     */
    public void markAsDeleted() {
        this.isDeleted = true;
    }

    /**
     * 삭제를 취소합니다.
     */
    public void unmarkAsDeleted() {
        this.isDeleted = false;
    }
} 