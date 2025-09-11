import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');

export let options = {
  stages: [
    { duration: '30s', target: 20 },  // 30초 동안 20명까지 점진적 증가
    { duration: '1m', target: 50 },   // 1분 동안 50명까지 증가
    { duration: '2m', target: 100 },  // 2분 동안 100명까지 증가 (피크)
    { duration: '1m', target: 50 },   // 1분 동안 50명으로 감소
    { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이하
    http_req_failed: ['rate<0.05'],   // 에러율 5% 이하
    errors: ['rate<0.1'],             // 커스텀 에러율 10% 이하
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';

// 테스트용 이벤트 ID (실제 환경에서는 미리 생성된 이벤트 사용)
const EVENT_ID = 1;

// 가상의 멤버 ID 풀 (실제로는 DB에서 가져와야 함)
const MEMBER_IDS = Array.from({length: 1000}, (_, i) => i + 1);

export function setup() {
  console.log('=== 이벤트 참여 부하 테스트 시작 ===');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Event ID: ${EVENT_ID}`);
  console.log(`Target Users: ${options.stages[2].target}`);
  
  // 테스트 시작 전 헬스체크
  let healthCheck = http.get(`${BASE_URL}/actuator/health`);
  console.log(`Health Check Status: ${healthCheck.status}`);
  
  return { eventId: EVENT_ID };
}

export default function(data) {
  const eventId = data.eventId;
  
  // 랜덤한 멤버 ID 선택
  const memberId = MEMBER_IDS[Math.floor(Math.random() * MEMBER_IDS.length)];
  
  // 이벤트 참여 요청
  const participatePayload = JSON.stringify({
    memberId: memberId
  });
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: {
      endpoint: 'participate',
      member: memberId,
    },
  };
  
  // 이벤트 참여 시도
  let participateResponse = http.post(
    `${BASE_URL}/api/events/${eventId}/participate`,
    participatePayload,
    params
  );
  
  // 응답 검증
  let participateSuccess = check(participateResponse, {
    'participate request status is 200 or 409': (r) => r.status === 200 || r.status === 409,
    'participate response time < 500ms': (r) => r.timings.duration < 500,
    'participate response has body': (r) => r.body.length > 0,
  });
  
  if (!participateSuccess) {
    errorRate.add(1);
    console.log(`Participate failed for member ${memberId}: ${participateResponse.status} - ${participateResponse.body}`);
  } else {
    errorRate.add(0);
  }
  
  // 잠시 대기 (1-3초 랜덤)
  sleep(Math.random() * 2 + 1);
  
  // 30% 확률로 참여자 목록 조회
  if (Math.random() < 0.3) {
    let participantsResponse = http.get(
      `${BASE_URL}/api/events/${eventId}/participants`,
      {
        tags: {
          endpoint: 'participants',
        },
      }
    );
    
    check(participantsResponse, {
      'participants request status is 200': (r) => r.status === 200,
      'participants response time < 200ms': (r) => r.timings.duration < 200,
    });
  }
  
  // 10% 확률로 참여 취소 시도
  if (Math.random() < 0.1) {
    let cancelResponse = http.del(
      `${BASE_URL}/api/events/${eventId}/participate`,
      null,
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: {
          endpoint: 'cancel',
          member: memberId,
        },
      }
    );
    
    check(cancelResponse, {
      'cancel request status is 200 or 404': (r) => r.status === 200 || r.status === 404,
      'cancel response time < 300ms': (r) => r.timings.duration < 300,
    });
  }
}

export function teardown(data) {
  console.log('=== 이벤트 참여 부하 테스트 완료 ===');
  
  // 테스트 완료 후 최종 참여자 수 확인
  let finalCheck = http.get(`${BASE_URL}/api/events/${data.eventId}/participants`);
  if (finalCheck.status === 200) {
    try {
      let participants = JSON.parse(finalCheck.body);
      console.log(`최종 참여자 수: ${participants.participants ? participants.participants.length : 'N/A'}`);
    } catch (e) {
      console.log('참여자 수 파싱 실패');
    }
  }
}