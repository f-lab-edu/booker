package com.bookerapp.core.domain.model.entity;

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
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String createdBy;

    @Column(length = 50)
    private String updatedBy;

    @Column(nullable = false)
    private boolean isDeleted;

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public void unmarkAsDeleted() {
        this.isDeleted = false;
    }
<<<<<<<< HEAD:booker-server/src/main/java/com/bookerapp/core/domain/model/BaseEntity.java
}
========
} 
>>>>>>>> b0cca449fc1eedbe7245826d588fe47133680064:booker-server/src/main/java/com/bookerapp/core/domain/model/entity/BaseEntity.java
