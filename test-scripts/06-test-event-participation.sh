#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing Event Participation API (/api/v1/events/participation)"

# Check if server is running
check_server || exit 1

# Prerequisite: Get an event ID for testing
print_info "Prerequisite: Getting an event ID for testing"
EVENTS_RESPONSE=$(curl -s "${BASE_URL}/api/v1/events?page=0&size=1")
EVENT_ID=$(echo "$EVENTS_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', [{}])[0].get('id', '1'))" 2>/dev/null || echo "1")
print_info "Using Event ID: ${EVENT_ID}"

# Test 1: Participate with Synchronized method
print_test_header "Test 1: Participate in Event (Synchronized)"

PARTICIPATION_DATA="{
  \"eventId\": ${EVENT_ID},
  \"participantId\": \"user-sync-1\",
  \"participantName\": \"동기화 테스트 사용자 1\",
  \"participantEmail\": \"sync1@example.com\"
}"

api_request POST "/api/v1/events/participation/synchronized?userId=user-sync-1" \
    "${PARTICIPATION_DATA}" \
    "Participating in event with Synchronized method"

# Test 2: Participate with CAS method
print_test_header "Test 2: Participate in Event (CAS)"

PARTICIPATION_DATA_2="{
  \"eventId\": ${EVENT_ID},
  \"participantId\": \"user-cas-1\",
  \"participantName\": \"CAS 테스트 사용자 1\",
  \"participantEmail\": \"cas1@example.com\"
}"

api_request POST "/api/v1/events/participation/cas?userId=user-cas-1" \
    "${PARTICIPATION_DATA_2}" \
    "Participating in event with CAS method"

# Test 3: Get CAS retry count
print_test_header "Test 3: Get CAS Retry Count"

api_request GET "/api/v1/events/participation/cas/retry-count" "" \
    "Getting CAS retry count"

# Test 4: Multiple participants with CAS
print_test_header "Test 4: Add Multiple Participants with CAS"

for i in {2..5}; do
    PARTICIPANT_DATA="{
      \"eventId\": ${EVENT_ID},
      \"participantId\": \"user-cas-${i}\",
      \"participantName\": \"CAS 사용자 ${i}\",
      \"participantEmail\": \"cas${i}@example.com\"
    }"

    print_info "Adding participant ${i}..."
    api_request POST "/api/v1/events/participation/cas?userId=user-cas-${i}" \
        "${PARTICIPANT_DATA}" \
        "Participant ${i} joining with CAS"

    sleep 0.5  # Small delay between requests
done

# Test 5: Get CAS retry count after multiple participations
print_test_header "Test 5: Get CAS Retry Count After Multiple Participations"

api_request GET "/api/v1/events/participation/cas/retry-count" "" \
    "Getting CAS retry count after multiple participations"

# Test 6: Try to participate again (should fail - duplicate)
print_test_header "Test 6: Try to Participate Again (Duplicate)"

api_request POST "/api/v1/events/participation/cas?userId=user-cas-1" \
    "${PARTICIPATION_DATA_2}" \
    "Attempting duplicate participation (expected to fail)"

# Test 7: Reset CAS retry count
print_test_header "Test 7: Reset CAS Retry Count"

api_request POST "/api/v1/events/participation/cas/reset-retry-count" "" \
    "Resetting CAS retry count"

# Test 8: Verify retry count is reset
print_test_header "Test 8: Verify CAS Retry Count is Reset"

api_request GET "/api/v1/events/participation/cas/retry-count" "" \
    "Getting CAS retry count after reset (should be 0)"

# Test 9: Add more participants with Synchronized
print_test_header "Test 9: Add Multiple Participants with Synchronized"

for i in {2..3}; do
    PARTICIPANT_DATA="{
      \"eventId\": ${EVENT_ID},
      \"participantId\": \"user-sync-${i}\",
      \"participantName\": \"동기화 사용자 ${i}\",
      \"participantEmail\": \"sync${i}@example.com\"
    }"

    print_info "Adding synchronized participant ${i}..."
    api_request POST "/api/v1/events/participation/synchronized?userId=user-sync-${i}" \
        "${PARTICIPANT_DATA}" \
        "Participant ${i} joining with Synchronized"

    sleep 0.5
done

# Test 10: Check event details to see participants
print_test_header "Test 10: Check Event Details with Participants"

api_request GET "/api/v1/events/${EVENT_ID}" "" \
    "Getting event details to see all participants"

echo ""
print_success "Event Participation API tests completed"
echo "Event ID: ${EVENT_ID}"
print_info "Synchronized participants: user-sync-1, user-sync-2, user-sync-3"
print_info "CAS participants: user-cas-1, user-cas-2, user-cas-3, user-cas-4, user-cas-5"
print_info "Total participants added: 8"
