package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class EventParticipationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        Member presenter = new Member("presenter1", "Presenter", "presenter@test.com");
        testEvent = new Event(
                "Test Event",
                "Test Description",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                5,
                presenter
        );
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    @DisplayName("Synchronized 방식 이벤트 참여 신청 API 테스트")
    void synchronizedParticipationApiTest() throws Exception {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "user1",
                "User One",
                "user1@test.com"
        );

        mockMvc.perform(post("/events/participation/synchronized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userContext", createUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.message").value("참여가 확정되었습니다."));
    }

    @Test
    @DisplayName("CAS 방식 이벤트 참여 신청 API 테스트")
    void casParticipationApiTest() throws Exception {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "user2",
                "User Two",
                "user2@test.com"
        );

        mockMvc.perform(post("/events/participation/cas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userContext", createUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.message").value("참여가 확정되었습니다."));
    }

    @Test
    @DisplayName("CAS 재시도 횟수 조회 API 테스트")
    void getCasRetryCountApiTest() throws Exception {
        mockMvc.perform(get("/events/participation/cas/retry-count")
                        .requestAttr("userContext", createAdminUserContext()))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("CAS 재시도 횟수 초기화 API 테스트")
    void resetCasRetryCountApiTest() throws Exception {
        mockMvc.perform(post("/events/participation/cas/reset-retry-count")
                        .requestAttr("userContext", createAdminUserContext()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("존재하지 않는 이벤트 참여 신청 시 에러 처리 테스트")
    void nonExistentEventParticipationTest() throws Exception {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                999L,
                "user3",
                "User Three",
                "user3@test.com"
        );

        mockMvc.perform(post("/events/participation/synchronized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userContext", createUserContext()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("대기자 명단 등록 테스트")
    void waitingListRegistrationTest() throws Exception {
        // 최대 참가자 수만큼 먼저 등록
        for (int i = 0; i < 5; i++) {
            EventParticipationDto.Request request = new EventParticipationDto.Request(
                    testEvent.getId(),
                    "user" + i,
                    "User " + i,
                    "user" + i + "@test.com"
            );

            mockMvc.perform(post("/events/participation/synchronized")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .requestAttr("userContext", createUserContext()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        // 추가 참가자는 대기자 명단에 등록
        EventParticipationDto.Request waitingRequest = new EventParticipationDto.Request(
                testEvent.getId(),
                "waitingUser",
                "Waiting User",
                "waiting@test.com"
        );

        mockMvc.perform(post("/events/participation/synchronized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waitingRequest))
                        .requestAttr("userContext", createUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.waitingNumber").value(1))
                .andExpect(jsonPath("$.message").value("대기자 명단에 등록되었습니다. 대기 순번: 1"));
    }

    @Test
    @DisplayName("중복 참여 신청 방지 테스트")
    void duplicateParticipationPreventionTest() throws Exception {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "duplicateUser",
                "Duplicate User",
                "duplicate@test.com"
        );

        // 첫 번째 참여 신청
        mockMvc.perform(post("/events/participation/synchronized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userContext", createUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 두 번째 참여 신청 (중복)
        mockMvc.perform(post("/events/participation/synchronized")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userContext", createUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_PARTICIPATING"))
                .andExpect(jsonPath("$.message").value("이미 참여 신청된 이벤트입니다."));
    }

    private UserContext createUserContext() {
        return new UserContext("test-user", "Test User", "test@test.com", List.of("USER"));
    }

    private UserContext createAdminUserContext() {
        return new UserContext("admin-user", "Admin User", "admin@test.com", List.of("ADMIN"));
    }
}
