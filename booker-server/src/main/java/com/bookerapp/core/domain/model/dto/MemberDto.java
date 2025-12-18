package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.event.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "MemberResponse", description = "회원 정보 응답")
public class MemberDto {

    @Schema(description = "회원 고유 ID (DB ID)", example = "1")
    private Long id;

    @Schema(description = "회원 사용자 ID", example = "test-user")
    private String memberId;

    @Schema(description = "회원 이름", example = "홍길동")
    private String name;

    @Schema(description = "회원 이메일", example = "test@example.com")
    private String email;

    @Schema(description = "부서명", example = "개발팀")
    private String department;

    @Schema(description = "직급", example = "시니어 개발자")
    private String position;

    public static MemberDto from(Member member) {
        if (member == null) {
            return null;
        }
        return MemberDto.builder()
                .id(member.getId())
                .memberId(member.getMemberId())
                .name(member.getName())
                .email(member.getEmail())
                .department(member.getDepartment())
                .position(member.getPosition())
                .build();
    }
}
