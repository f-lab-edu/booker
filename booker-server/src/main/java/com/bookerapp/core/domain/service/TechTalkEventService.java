package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.dto.EventDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.EventRepository;
import org.springframework.stereotype.Service;

@Service
public class TechTalkEventService extends AbstractEventService {

    public TechTalkEventService(EventRepository eventRepository) {
        super(eventRepository);
    }

    @Override
    protected void handleEventCreation(Event event, EventDto.CreateRequest request) {
        // TechTalk specific event creation logic
        System.out.println("TechTalk 이벤트 생성: " + event.getTitle());
    }

    @Override
    protected void handleEventUpdate(Event event, EventDto.UpdateRequest request) {
        // TechTalk specific event update logic
        System.out.println("TechTalk 이벤트 업데이트: " + event.getTitle());
    }

    @Override
    protected void handleEventDeletion(Event event) {
        // TechTalk specific event deletion logic
        System.out.println("TechTalk 이벤트 삭제: " + event.getTitle());
    }

    @Override
    protected void handleParticipantAddition(Event event, Member member) {
        // Send notification to participant
        System.out.println("참가자 추가 알림: " + member.getName() + "님이 " + event.getTitle() + "에 참가했습니다.");
    }

    @Override
    protected void handleParticipantRemoval(Event event, Member member) {
        // Send notification to participant
        System.out.println("참가자 제거 알림: " + member.getName() + "님이 " + event.getTitle() + "에서 제거되었습니다.");
    }
}
