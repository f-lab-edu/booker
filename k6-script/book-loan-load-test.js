import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from "https://jslib.k6.io/k6-utils/1.1.0/index.js";

export let options = {
    vus: 10, // 가상 사용자 수
    duration: '5m', // 테스트 지속 시간
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 이내
        http_req_failed: ['rate<0.1'], // 실패율 10% 미만
    },
};

const KEYCLOAK_URL = 'http://localhost:8083';
const BOOKER_API_URL = 'http://localhost:8084';
const CLIENT_ID = 'springboot-client';
const CLIENT_SECRET = 'springboot-secret';
const REALM = 'myrealm';

// 토큰 획득 함수
function getClientToken() {
    const tokenParams = {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
    };

    const tokenBody = `grant_type=client_credentials&client_id=${CLIENT_ID}&client_secret=${CLIENT_SECRET}`;

    const tokenResponse = http.post(
        `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
        tokenBody,
        tokenParams
    );

    check(tokenResponse, {
        'token request successful': (r) => r.status === 200,
    });

    if (tokenResponse.status === 200) {
        const tokenData = JSON.parse(tokenResponse.body);
        return tokenData.access_token;
    }

    console.error('토큰 획득 실패:', tokenResponse.status, tokenResponse.body);
    return null;
}

// 이용 가능한 책 목록 조회
function getAvailableBooks(token) {
    const params = {
        headers: {
            'Accept': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };

    const response = http.get(`${BOOKER_API_URL}/api/v1/books?page=0&size=50`, params);

    if (response.status === 200) {
        const data = JSON.parse(response.body);
        return data.content || [];
    }

    console.error('책 목록 조회 실패:', response.status, response.body);
    return [];
}


// main 함수
export default function () {
    // 토큰 획득
    const token = getClientToken();

    if (!token) {
        console.error('토큰을 획득할 수 없어 테스트를 중단합니다.');
        return;
    }

    // 이용 가능한 책 목록 조회
    const availableBooks = getAvailableBooks(token);

    if (availableBooks.length === 0) {
        console.error('이용 가능한 책이 없습니다.');
        return;
    }

    // 랜덤하게 책 선택 (AVAILABLE 상태인 책만)
    const availableBooksOnly = availableBooks.filter(book => book.status === 'AVAILABLE');

    if (availableBooksOnly.length === 0) {
        console.error('대출 가능한 책이 없습니다.');
        return;
    }

    const randomBook = availableBooksOnly[randomIntBetween(0, availableBooksOnly.length - 1)];

    // 대출 신청 요청 데이터
    const loanRequestData = {
        bookId: 2
    };

    // JSON 바디 구성
    const body = JSON.stringify(loanRequestData);

    // HTTP POST 요청 설정
    const params = {
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };

    // 도서 대출 신청 POST 요청
    const response = http.post(`${BOOKER_API_URL}/api/v1/loans`, body, params);

    // 응답 확인
    const checkResult = check(response, {
        'status is 201': (r) => r.status === 201,
        'response has body': (r) => r.body && r.body.length > 0,
        'response is valid JSON': (r) => {
            try {
                JSON.parse(r.body);
                return true;
            } catch (e) {
                return false;
            }
        },
        'loan has ID': (r) => {
            try {
                const data = JSON.parse(r.body);
                return data.id !== undefined;
            } catch (e) {
                return false;
            }
        },
        'loan has correct bookId': (r) => {
            try {
                const data = JSON.parse(r.body);
                return data.bookId === randomBook.id;
            } catch (e) {
                return false;
            }
        },
    });

    // 실패한 경우 로그 출력
    if (!checkResult['status is 201']) {
        console.error(`대출 신청 실패 - Status: ${response.status}, Body: ${response.body}`);
        console.error(`Request data: ${body}`);
        console.error(`Selected book: ${JSON.stringify(randomBook)}`);
    }

    // 성공한 경우 로그 출력
    if (checkResult['status is 201']) {
        const loanData = JSON.parse(response.body);
        console.log(`대출 신청 성공 - Loan ID: ${loanData.id}, Book: "${randomBook.title}"`);
    }

    // 요청 간 간격 (Rate limiting 고려)
    sleep(0.1);
}

// setup 함수 - 테스트 시작 전 실행
export function setup() {
    console.log('=== K6 도서 대출 성능 테스트 시작 ===');
    console.log(`Keycloak URL: ${KEYCLOAK_URL}`);
    console.log(`Booker API URL: ${BOOKER_API_URL}`);

    // 초기 토큰 테스트
    const token = getClientToken();
    if (!token) {
        throw new Error('초기 토큰 획득에 실패했습니다. Keycloak 서버 상태를 확인하세요.');
    }

    // 이용 가능한 책이 있는지 확인
    const availableBooks = getAvailableBooks(token);
    if (availableBooks.length === 0) {
        throw new Error('테스트할 책이 없습니다. 먼저 책을 등록해주세요.');
    }

    console.log(`이용 가능한 책 수: ${availableBooks.length}`);
    console.log('토큰 획득 성공 - 테스트 준비 완료');
    return { token, availableBooks: availableBooks.length };
}

// teardown 함수 - 테스트 종료 후 실행
export function teardown(data) {
    console.log('=== K6 도서 대출 성능 테스트 완료 ===');
    console.log(`초기 이용 가능한 책 수: ${data.availableBooks}`);
}
