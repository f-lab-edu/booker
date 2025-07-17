package com.bookerapp.core.infrastructure.client;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Collections;
import com.google.api.services.calendar.CalendarScopes;

@Slf4j
@Component
public class GoogleCalendarClient {

    @Value("${google.calendar.credentials-file-path}")
    private String credentialsFilePath;

    @Value("${google.calendar.application-name}")
    private String applicationName;

    @Value("${google.calendar.calendar-id}")
    private String calendarId;

    private Calendar service;

    public void init() throws Exception {
        GoogleCredential credential = GoogleCredential
                .fromStream(new FileInputStream(credentialsFilePath))
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

        service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(applicationName)
                .build();
    }

    public String createEvent(String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Event event = new Event()
                    .setSummary(title)
                    .setDescription(description);

            EventDateTime start = new EventDateTime()
                    .setDateTime(convertToDate(startTime));
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(convertToDate(endTime));
            event.setEnd(end);

            event = service.events().insert(calendarId, event).execute();
            return event.getId();
        } catch (Exception e) {
            log.error("Failed to create Google Calendar event", e);
            throw new RuntimeException("Failed to create Google Calendar event", e);
        }
    }

    public void updateEvent(String eventId, String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Event event = service.events().get(calendarId, eventId).execute();
            event.setSummary(title)
                    .setDescription(description);

            EventDateTime start = new EventDateTime()
                    .setDateTime(convertToDate(startTime));
            event.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(convertToDate(endTime));
            event.setEnd(end);

            service.events().update(calendarId, eventId, event).execute();
        } catch (Exception e) {
            log.error("Failed to update Google Calendar event", e);
            throw new RuntimeException("Failed to update Google Calendar event", e);
        }
    }

    public void deleteEvent(String eventId) {
        try {
            service.events().delete(calendarId, eventId).execute();
        } catch (Exception e) {
            log.error("Failed to delete Google Calendar event", e);
            throw new RuntimeException("Failed to delete Google Calendar event", e);
        }
    }

    private Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
} 