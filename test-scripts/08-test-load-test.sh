#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing LoadTest API (/api/load-test)"

# Check if server is running
check_server || exit 1

# Test 1: Health check
print_test_header "Test 1: Health Check"

api_request GET "/api/load-test/health" "" \
    "Checking load test controller health"

# Test 2: Setup test data
print_test_header "Test 2: Setup Test Data"

SETUP_DATA='{
  "eventId": 999,
  "eventTitle": "LoadTest Event",
  "maxParticipants": 10
}'

api_request POST "/api/load-test/setup" \
    "${SETUP_DATA}" \
    "Setting up load test data"

# Get the event ID from setup (should be 999 or the created one)
TEST_EVENT_ID=999

# Test 3: Participate with Optimistic Lock
print_test_header "Test 3: Participate with Optimistic Lock"

PARTICIPATION_DATA="{
  \"eventId\": ${TEST_EVENT_ID},
  \"participantId\": \"loadtest-opt-1\",
  \"participantName\": \"Optimistic User 1\",
  \"participantEmail\": \"opt1@loadtest.com\"
}"

api_request POST "/api/load-test/participate/optimistic" \
    "${PARTICIPATION_DATA}" \
    "Participating with optimistic lock"

# Test 4: Participate with Pessimistic Lock
print_test_header "Test 4: Participate with Pessimistic Lock"

PARTICIPATION_DATA_2="{
  \"eventId\": ${TEST_EVENT_ID},
  \"participantId\": \"loadtest-pess-1\",
  \"participantName\": \"Pessimistic User 1\",
  \"participantEmail\": \"pess1@loadtest.com\"
}"

api_request POST "/api/load-test/participate/pessimistic" \
    "${PARTICIPATION_DATA_2}" \
    "Participating with pessimistic lock"

# Test 5: Participate with CAS
print_test_header "Test 5: Participate with CAS"

PARTICIPATION_DATA_3="{
  \"eventId\": ${TEST_EVENT_ID},
  \"participantId\": \"loadtest-cas-1\",
  \"participantName\": \"CAS User 1\",
  \"participantEmail\": \"cas1@loadtest.com\"
}"

api_request POST "/api/load-test/participate/cas" \
    "${PARTICIPATION_DATA_3}" \
    "Participating with CAS"

# Test 6: Participate with Synchronized
print_test_header "Test 6: Participate with Synchronized"

PARTICIPATION_DATA_4="{
  \"eventId\": ${TEST_EVENT_ID},
  \"participantId\": \"loadtest-sync-1\",
  \"participantName\": \"Synchronized User 1\",
  \"participantEmail\": \"sync1@loadtest.com\"
}"

api_request POST "/api/load-test/participate/synchronized" \
    "${PARTICIPATION_DATA_4}" \
    "Participating with synchronized"

# Test 7: Multiple participations with different methods
print_test_header "Test 7: Multiple Participations"

METHODS=("optimistic" "pessimistic" "cas" "synchronized")

for i in {2..3}; do
    for method in "${METHODS[@]}"; do
        PARTICIPANT_DATA="{
          \"eventId\": ${TEST_EVENT_ID},
          \"participantId\": \"loadtest-${method}-${i}\",
          \"participantName\": \"${method} User ${i}\",
          \"participantEmail\": \"${method}${i}@loadtest.com\"
        }"

        print_info "Adding participant with ${method} method..."
        api_request POST "/api/load-test/participate/${method}" \
            "${PARTICIPANT_DATA}" \
            "Participant ${i} with ${method}"

        sleep 0.3
    done
done

# Test 8: Check event to see all participants
print_test_header "Test 8: Check Event Participants"

api_request GET "/api/v1/events/${TEST_EVENT_ID}" "" \
    "Getting event ${TEST_EVENT_ID} to see all participants"

# Test 9: Cleanup test data (commented out to preserve data)
print_test_header "Test 9: Cleanup Test Data"
print_info "Cleanup is skipped to preserve test data"
# api_request POST "/api/load-test/cleanup" "" \
#     "Cleaning up load test data"

echo ""
print_success "LoadTest API tests completed"
echo "Test Event ID: ${TEST_EVENT_ID}"
print_info "Tested 4 concurrency control methods:"
print_info "1. Optimistic Lock"
print_info "2. Pessimistic Lock"
print_info "3. CAS (Compare-And-Swap)"
print_info "4. Synchronized"
print_info ""
print_info "Total participants added: ~12-16 (3 per method x 4 methods)"
