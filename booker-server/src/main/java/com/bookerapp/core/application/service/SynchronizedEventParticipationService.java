package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import com.bookerapp.core.domain.repository.EventRepository;
import com.bookerapp.core.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SynchronizedEventParticipationService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public synchronized EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("Synchronized participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Member member = memberRepository.findByMemberId(request.getMemberId())
                .orElseGet(() -> {
                    Member newMember = new Member(request.getMemberId(), request.getMemberName(), request.getMemberEmail());
                    return memberRepository.save(newMember);
                });

        if (isAlreadyParticipating(event, member)) {
            return new EventParticipationDto.Response(null, "ALREADY_PARTICIPATING", null, "이미 참여 신청된 이벤트입니다.");
        }

        if (event.isFullyBooked()) {
            int nextWaitingNumber = getNextWaitingNumber(event);
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.WAITING, nextWaitingNumber);
            event.getParticipants().add(participation);

            log.info("Added to waiting list - Event: {}, Member: {}, Waiting Number: {}",
                    request.getEventId(), request.getMemberId(), nextWaitingNumber);

            return new EventParticipationDto.Response(participation.getId(), "WAITING", nextWaitingNumber,
                    "대기자 명단에 등록되었습니다. 대기 순번: " + nextWaitingNumber);
        } else {
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);

            log.info("Confirmed participation - Event: {}, Member: {}", request.getEventId(), request.getMemberId());

            return new EventParticipationDto.Response(participation.getId(), "CONFIRMED", null, "참여가 확정되었습니다.");
        }
    }

    private boolean isAlreadyParticipating(Event event, Member member) {
        return event.getParticipants().stream()
                .anyMatch(p -> p.getParticipant().equals(member) &&
                        (p.getStatus() == ParticipationStatus.CONFIRMED || p.getStatus() == ParticipationStatus.WAITING));
    }

    private int getNextWaitingNumber(Event event) {
        return event.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.WAITING)
                .map(EventParticipation::getWaitingNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
