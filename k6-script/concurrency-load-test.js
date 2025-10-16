import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { randomIntBetween } from "https://jslib.k6.io/k6-utils/1.1.0/index.js";
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

export let options = {
    scenarios: {
        // 낮은 부하 테스트 (동시성 문제가 발생하지 않을 수준)
        low_load: {
            executor: 'constant-vus',
            vus: 5,
            duration: '30s',
            tags: { load_type: 'low' },
        },
        // 중간 부하 테스트 (일부 동시성 문제 발생 가능)
        medium_load: {
            executor: 'constant-vus',
            vus: 20,
            duration: '60s',
            startTime: '35s',
            tags: { load_type: 'medium' },
        },
        // 높은 부하 테스트 (동시성 문제가 확실히 발생할 수준)
        high_load: {
            executor: 'constant-vus',
            vus: 50,
            duration: '120s',
            startTime: '100s',
            tags: { load_type: 'high' },
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<5000'], // 95%의 요청이 5초 이내
        http_req_failed: ['rate<0.05'], // 실패율 5% 미만
        'http_req_duration{service:optimistic}': ['p(95)<3000'],
        'http_req_duration{service:pessimistic}': ['p(95)<5000'],
        'http_req_duration{service:cas}': ['p(95)<3000'],
        'http_req_duration{service:synchronized}': ['p(95)<2000'],
    },
};

const BOOKER_API_URL = 'http://localhost:8084';
const LOAD_TEST_BASE_URL = `${BOOKER_API_URL}/api/load-test`;

// 이벤트 참여 서비스 타입
const SERVICES = [
    { name: 'optimistic', endpoint: '/participate/optimistic' },
    { name: 'pessimistic', endpoint: '/participate/pessimistic' },
    { name: 'cas', endpoint: '/participate/cas' },
    { name: 'synchronized', endpoint: '/participate/synchronized' },
];

// 테스트용 이벤트 ID (setup 함수에서 동적으로 설정됨)
let EVENT_ID = 1; // 초기값, setup에서 실제 값으로 변경됨

// 공통 참여 요청 함수
function participateInEvent(service, eventId, userId) {
    const requestData = {
        eventId: eventId,
        userId: userId
    };

    const params = {
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
        },
        tags: {
            service: service.name,
            endpoint: service.endpoint,
        },
    };

    const response = http.post(
        `${LOAD_TEST_BASE_URL}${service.endpoint}`,
        JSON.stringify(requestData),
        params
    );

    return response;
}

// 재시도 메트릭 조회 함수
function getRetryMetrics(serviceName) {
    const response = http.get(`${LOAD_TEST_BASE_URL}/metrics/${serviceName}`);

    if (response.status === 200) {
        try {
            const data = JSON.parse(response.body);
            return data.retryCount || 0;
        } catch (e) {
            console.error(`Failed to parse retry metrics for ${serviceName}:`, e);
            return 0;
        }
    }

    console.error(`Failed to get retry metrics for ${serviceName}:`, response.status, response.body);
    return 0;
}

// 재시도 카운터 리셋 함수
function resetRetryCounter(serviceName) {
    const response = http.post(`${LOAD_TEST_BASE_URL}/reset/${serviceName}`);

    check(response, {
        [`${serviceName} reset successful`]: (r) => r.status === 200,
    });

    return response.status === 200;
}

// 헬스 체크 함수
function healthCheck() {
    const response = http.get(`${LOAD_TEST_BASE_URL}/health`);

    check(response, {
        'health check successful': (r) => r.status === 200,
    });

    return response.status === 200;
}

// 테스트 데이터 설정 함수
function setupTestData() {
    const setupData = {
        eventId: EVENT_ID,
        eventTitle: "LoadTest Event - Concurrency Comparison",
        maxParticipants: 10
    };

    const response = http.post(
        `${LOAD_TEST_BASE_URL}/setup`,
        JSON.stringify(setupData),
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    check(response, {
        'setup successful': (r) => r.status === 200,
        'setup response valid': (r) => {
            try {
                const data = JSON.parse(r.body);
                return data.eventId !== null && data.eventId !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    if (response.status === 200) {
        try {
            const data = JSON.parse(response.body);
            EVENT_ID = data.eventId; // 실제 생성된 이벤트 ID 사용
            console.log(`✅ Test data setup completed for event ${EVENT_ID}`);
            return EVENT_ID;
        } catch (e) {
            console.error('❌ Failed to parse setup response:', e);
            throw new Error('Setup response parsing failed');
        }
    } else {
        console.error(`❌ Test data setup failed: ${response.status} - ${response.body}`);
        throw new Error('Test data setup failed');
    }
}

// 메인 테스트 함수
export default function (data) {
    // 각 VU마다 고유한 사용자 ID 생성
    const userId = randomIntBetween(1, 10000);

    // 사용할 서비스 랜덤 선택 (모든 서비스를 고르게 테스트)
    const service = SERVICES[__VU % SERVICES.length];

    // setup()에서 전달받은 실제 이벤트 ID 사용
    const eventId = data ? data.eventId : EVENT_ID;

    group(`${service.name} participation test`, () => {
        const response = participateInEvent(service, eventId, userId);

        const checkResults = check(response, {
            'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
            'response has body': (r) => r.body && r.body.length > 0,
            'response is valid JSON': (r) => {
                try {
                    JSON.parse(r.body);
                    return true;
                } catch (e) {
                    return false;
                }
            },
        });

        // 성공/실패 로깅
        if (response.status === 200) {
            console.log(`✅ ${service.name}: User ${userId} participated successfully`);
        } else if (response.status === 409) {
            console.log(`⚠️ ${service.name}: User ${userId} already participated or capacity full`);
        } else {
            console.error(`❌ ${service.name}: User ${userId} failed - Status: ${response.status}, Body: ${response.body}`);
        }
    });

    // 요청 간 간격 (과도한 부하 방지)
    sleep(0.1);
}

// 테스트 시작 전 설정
export function setup() {
    console.log('=== K6 동시성 제어 메커니즘 비교 테스트 시작 ===');
    console.log(`Test Target: ${BOOKER_API_URL}`);
    console.log(`Event ID: ${EVENT_ID}`);

    // 헬스 체크
    if (!healthCheck()) {
        throw new Error('Load test controller health check failed. 서버 상태를 확인하세요.');
    }

    // 테스트 데이터 설정
    console.log('Setting up test data...');
    const actualEventId = setupTestData();

    // 모든 재시도 카운터 리셋
    console.log('Resetting retry counters...');
    resetRetryCounter('optimistic');
    resetRetryCounter('cas');

    console.log('테스트 준비 완료');

    return {
        startTime: new Date().toISOString(),
        eventId: actualEventId,
    };
}

// 테스트 종료 후 처리
export function teardown(data) {
    console.log('=== K6 동시성 제어 메커니즘 비교 테스트 완료 ===');
    console.log(`테스트 시작: ${data.startTime}`);
    console.log(`테스트 종료: ${new Date().toISOString()}`);

    // 최종 재시도 메트릭 수집
    console.log('\n=== 재시도 메트릭 수집 ===');

    const optimisticRetries = getRetryMetrics('optimistic');
    const casRetries = getRetryMetrics('cas');

    console.log(`Optimistic Locking 재시도 횟수: ${optimisticRetries}`);
    console.log(`CAS 재시도 횟수: ${casRetries}`);

    // 결과 요약
    console.log('\n=== 테스트 결과 요약 ===');
    console.log('각 동시성 제어 메커니즘의 성능을 Grafana 대시보드에서 확인하세요.');
    console.log('- Response Time: 응답 시간 비교');
    console.log('- Throughput: 처리량 비교');
    console.log('- Error Rate: 오류율 비교');
    console.log('- Retry Count: 재시도 횟수 비교 (Optimistic, CAS)');
}

// HTML 리포트 생성
export function handleSummary(data) {
    return {
        "summary.html": htmlReport(data),
        stdout: textSummary(data, { indent: " ", enableColors: true }),
    };
}
