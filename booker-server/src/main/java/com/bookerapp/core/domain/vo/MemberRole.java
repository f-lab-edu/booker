package com.bookerapp.core.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 역할을 정의하는 열거형
 * Keycloak의 역할과 매핑되어 권한 관리에 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum MemberRole {
    
    /**
     * 일반 사용자 - 도서 대출/반납, 이벤트 참여 등 기본 기능 사용 가능
     */
    USER("USER", "일반 사용자"),
    
    /**
     * 관리자 - 시스템 전체 관리 권한 보유
     */
    ADMIN("ADMIN", "관리자");

    private final String code;
    private final String description;

    /**
     * 코드 값으로 MemberRole을 찾습니다.
     * 
     * @param code 역할 코드
     * @return MemberRole 또는 null
     */
    public static MemberRole fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (MemberRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 관리자 권한 여부를 확인합니다.
     * 
     * @return 관리자인 경우 true
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 일반 사용자 권한 여부를 확인합니다.
     * 
     * @return 일반 사용자인 경우 true
     */
    public boolean isUser() {
        return this == USER;
    }
} 