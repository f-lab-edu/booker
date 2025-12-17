#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing Event API (/api/v1/events)"

# Check if server is running
check_server || exit 1

# Test 1: Create an event
print_test_header "Test 1: Create Event"

EVENT_DATA='{
  "title": "Spring Boot 심화 스터디",
  "description": "Spring Boot의 고급 기능을 학습하는 스터디입니다.",
  "type": "STUDY_GROUP",
  "startTime": "2025-12-20T19:00:00",
  "endTime": "2025-12-20T21:00:00",
  "maxParticipants": 10
}'

CREATE_RESPONSE=$(api_request POST "/api/v1/events?userId=${TEST_USER_ID}&username=${TEST_USER_NAME}&email=${TEST_USER_EMAIL}" \
    "${EVENT_DATA}" \
    "Creating a new event")

EVENT_ID=$(echo "$CREATE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$EVENT_ID" ]; then
    print_success "Event created with ID: ${EVENT_ID}"
else
    print_error "Failed to extract event ID from response"
    EVENT_ID="1"  # Fallback ID for testing
fi

# Test 2: Get all events
print_test_header "Test 2: Get All Events"

api_request GET "/api/v1/events?page=0&size=20" "" \
    "Getting all events"

# Test 3: Get events by type
print_test_header "Test 3: Get Events by Type (STUDY_GROUP)"

api_request GET "/api/v1/events?type=STUDY_GROUP&page=0&size=20" "" \
    "Getting STUDY_GROUP events"

# Test 4: Get event details
print_test_header "Test 4: Get Event Details"

api_request GET "/api/v1/events/${EVENT_ID}" "" \
    "Getting details for event ${EVENT_ID}"

# Test 5: Update event
print_test_header "Test 5: Update Event"

UPDATE_DATA='{
  "title": "Spring Boot 심화 스터디 (온라인)",
  "description": "Spring Boot의 고급 기능을 학습하는 온라인 스터디입니다.",
  "startTime": "2025-12-20T19:30:00",
  "endTime": "2025-12-20T21:30:00"
}'

api_request PUT "/api/v1/events/${EVENT_ID}" \
    "${UPDATE_DATA}" \
    "Updating event ${EVENT_ID}"

# Test 6: Add participant to event (admin)
print_test_header "Test 6: Add Participant to Event"

api_request POST "/api/v1/events/${EVENT_ID}/participants?memberId=participant1&memberName=홍길동&memberEmail=hong@example.com" \
    "" \
    "Adding participant to event ${EVENT_ID}"

# Create more events for testing
print_test_header "Creating Additional Test Events"

EVENT_DATA_2='{
  "title": "React 기초 워크샵",
  "description": "React의 기초를 배우는 워크샵입니다.",
  "type": "WORKSHOP",
  "startTime": "2025-12-22T14:00:00",
  "endTime": "2025-12-22T17:00:00",
  "maxParticipants": 20
}'

CREATE_RESPONSE_2=$(api_request POST "/api/v1/events?userId=${TEST_USER_ID}&username=${TEST_USER_NAME}&email=${TEST_USER_EMAIL}" \
    "${EVENT_DATA_2}" \
    "Creating workshop event")

EVENT_ID_2=$(echo "$CREATE_RESPONSE_2" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

EVENT_DATA_3='{
  "title": "MSA 아키텍처 기술 발표",
  "description": "마이크로서비스 아키텍처에 대한 기술 발표입니다.",
  "type": "TECH_TALK",
  "startTime": "2025-12-25T15:00:00",
  "endTime": "2025-12-25T16:30:00",
  "maxParticipants": 50
}'

CREATE_RESPONSE_3=$(api_request POST "/api/v1/events?userId=${TEST_USER_ID}&username=${TEST_USER_NAME}&email=${TEST_USER_EMAIL}" \
    "${EVENT_DATA_3}" \
    "Creating tech talk event")

EVENT_ID_3=$(echo "$CREATE_RESPONSE_3" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

# Test 7: Get events by different types
print_test_header "Test 7: Get WORKSHOP Events"

api_request GET "/api/v1/events?type=WORKSHOP&page=0&size=20" "" \
    "Getting WORKSHOP events"

print_test_header "Test 8: Get TECH_TALK Events"

api_request GET "/api/v1/events?type=TECH_TALK&page=0&size=20" "" \
    "Getting TECH_TALK events"

# Test 9: Test sorting
print_test_header "Test 9: Get Events Sorted by Start Time"

api_request GET "/api/v1/events?page=0&size=20&sort=startTime,asc" "" \
    "Getting events sorted by startTime ascending"

# Test 10: Remove participant (commented out to preserve test data)
print_test_header "Test 10: Remove Participant from Event"
print_info "Remove participant test is skipped to preserve test data"
# api_request DELETE "/api/v1/events/${EVENT_ID}/participants/participant1?memberName=홍길동&memberEmail=hong@example.com" \
#     "" \
#     "Removing participant from event ${EVENT_ID}"

# Test 11: Delete event (commented out to preserve test data)
print_test_header "Test 11: Delete Event"
print_info "Delete event test is skipped to preserve test data"
# api_request DELETE "/api/v1/events/${EVENT_ID_3}" "" \
#     "Deleting event ${EVENT_ID_3}"

echo ""
print_success "Event API tests completed"
echo "Created Event IDs: ${EVENT_ID}, ${EVENT_ID_2}, ${EVENT_ID_3}"
print_info "Event Types: STUDY_GROUP, WORKSHOP, TECH_TALK"
