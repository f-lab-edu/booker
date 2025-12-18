package com.bookerapp.core.domain.repository;

import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EventParticipation Repository
 * 이벤트 참여 정보에 대한 데이터 액세스 레이어
 */
@Repository
public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    /**
     * 중복 참여 확인
     * 특정 이벤트에 특정 회원이 이미 참여했는지 확인
     *
     * @param eventId 이벤트 ID
     * @param memberId 회원 ID (Member.memberId)
     * @return 참여 여부
     */
    @Query("SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END FROM EventParticipation ep " +
           "WHERE ep.event.id = :eventId AND ep.participant.memberId = :memberId")
    boolean existsByEventIdAndParticipantMemberId(@Param("eventId") Long eventId,
                                                    @Param("memberId") String memberId);

    /**
     * 이벤트별 특정 상태의 참여자 수 조회
     *
     * @param eventId 이벤트 ID
     * @param status 참여 상태
     * @return 참여자 수
     */
    @Query("SELECT COUNT(ep) FROM EventParticipation ep " +
           "WHERE ep.event.id = :eventId AND ep.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId,
                                  @Param("status") ParticipationStatus status);

    /**
     * 회원의 이벤트 참여 내역 조회
     *
     * @param memberId 회원 ID (Member.memberId)
     * @return 참여 내역 목록
     */
    @Query("SELECT ep FROM EventParticipation ep " +
           "WHERE ep.participant.memberId = :memberId " +
           "ORDER BY ep.registrationDate DESC")
    List<EventParticipation> findByParticipantMemberId(@Param("memberId") String memberId);

    /**
     * 이벤트별 특정 상태의 참여 내역 조회 (신청일시 순)
     *
     * @param eventId 이벤트 ID
     * @param status 참여 상태
     * @return 참여 내역 목록
     */
    @Query("SELECT ep FROM EventParticipation ep " +
           "WHERE ep.event.id = :eventId AND ep.status = :status " +
           "ORDER BY ep.registrationDate ASC")
    List<EventParticipation> findByEventIdAndStatusOrderByRegistrationDateAsc(
            @Param("eventId") Long eventId,
            @Param("status") ParticipationStatus status);

    /**
     * 이벤트와 회원으로 참여 정보 조회
     *
     * @param eventId 이벤트 ID
     * @param memberId 회원 ID (Member.memberId)
     * @return 참여 정보
     */
    @Query("SELECT ep FROM EventParticipation ep " +
           "WHERE ep.event.id = :eventId AND ep.participant.memberId = :memberId")
    Optional<EventParticipation> findByEventIdAndParticipantMemberId(
            @Param("eventId") Long eventId,
            @Param("memberId") String memberId);
}
