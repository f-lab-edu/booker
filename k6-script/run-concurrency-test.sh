#!/bin/bash

# K6 동시성 제어 메커니즘 비교 테스트 실행 스크립트

set -e

echo "=== Booker 동시성 제어 메커니즘 K6 로드 테스트 ==="
echo "테스트 시작 시간: $(date)"
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_SCRIPT="$SCRIPT_DIR/concurrency-load-test.js"
OUTPUT_DIR="$SCRIPT_DIR/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 출력 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

# 서버 상태 확인 함수
check_server_health() {
    echo -e "${BLUE}서버 상태 확인 중...${NC}"

    # Spring Boot 서버 확인
    if ! curl -s -f "http://localhost:8084/api/load-test/health" > /dev/null; then
        echo -e "${RED}❌ Spring Boot 서버가 실행되지 않았습니다. (포트 8084)${NC}"
        echo "다음 명령어로 서버를 시작하세요:"
        echo "  cd booker-server && ./gradlew bootRun"
        exit 1
    fi

    # Load Test Controller 확인
    if ! curl -s -f "http://localhost:8084/api/load-test/health" > /dev/null; then
        echo -e "${RED}❌ Load Test Controller가 응답하지 않습니다.${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ 서버 상태 정상${NC}"
}

# K6 설치 확인 함수
check_k6_installation() {
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}❌ K6가 설치되지 않았습니다.${NC}"
        echo "K6 설치 방법:"
        echo "  macOS: brew install k6"
        echo "  Ubuntu/Debian: sudo apt update && sudo apt install k6"
        echo "  또는 https://k6.io/docs/get-started/installation/ 참조"
        exit 1
    fi

    echo -e "${GREEN}✅ K6 설치 확인됨 ($(k6 version))${NC}"
}

# 사전 요구사항 확인
prereq_check() {
    echo -e "${BLUE}사전 요구사항 확인 중...${NC}"
    check_k6_installation
    check_server_health
    echo ""
}

# 테스트 실행 함수
run_test() {
    local test_name="$1"
    local output_file="$OUTPUT_DIR/concurrency_test_${test_name}_${TIMESTAMP}"

    echo -e "${BLUE}테스트 실행: $test_name${NC}"
    echo "출력 파일: $output_file"
    echo ""

    # K6 테스트 실행
    k6 run \
        --out json="$output_file.json" \
        --out influxdb=http://localhost:8086/myk6db \
        --summary-export="$output_file.summary.json" \
        "$TEST_SCRIPT" 2>&1 | tee "$output_file.log"

    local exit_code=${PIPESTATUS[0]}

    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✅ 테스트 완료: $test_name${NC}"

        # HTML 리포트가 생성되었다면 복사
        if [ -f "summary.html" ]; then
            mv "summary.html" "$output_file.html"
            echo -e "${GREEN}📊 HTML 리포트: $output_file.html${NC}"
        fi
    else
        echo -e "${RED}❌ 테스트 실패: $test_name (exit code: $exit_code)${NC}"
        return $exit_code
    fi

    echo ""
}

# 결과 분석 함수
analyze_results() {
    echo -e "${BLUE}=== 테스트 결과 분석 ===${NC}"

    local latest_summary=$(ls -t "$OUTPUT_DIR"/*.summary.json 2>/dev/null | head -1)

    if [ -n "$latest_summary" ] && [ -f "$latest_summary" ]; then
        echo "최신 테스트 결과 요약:"
        echo "파일: $latest_summary"
        echo ""

        # jq가 설치되어 있다면 결과 파싱
        if command -v jq &> /dev/null; then
            echo "주요 메트릭:"
            jq -r '
                .metrics | to_entries[] |
                select(.key | test("http_req_duration|http_req_failed|checks")) |
                "\(.key): \(.value.avg // .value.rate // .value.passes) \(.value.unit // "")"
            ' "$latest_summary" 2>/dev/null || echo "메트릭 파싱 실패"
        else
            echo "상세 분석을 위해 jq를 설치하세요: brew install jq"
        fi
    else
        echo "분석할 결과 파일을 찾을 수 없습니다."
    fi

    echo ""
    echo -e "${YELLOW}추가 분석을 위해 다음 파일들을 확인하세요:${NC}"
    echo "  - JSON 결과: $OUTPUT_DIR/*.json"
    echo "  - HTML 리포트: $OUTPUT_DIR/*.html"
    echo "  - 로그 파일: $OUTPUT_DIR/*.log"
    echo ""
    echo -e "${YELLOW}Grafana 대시보드에서 실시간 메트릭을 확인하세요:${NC}"
    echo "  - URL: http://localhost:3000"
    echo "  - 사용자명/비밀번호: admin/admin"
}

# 메인 실행
main() {
    echo "동시성 제어 메커니즘 비교 테스트를 시작합니다..."
    echo ""

    # 사전 요구사항 확인
    prereq_check

    # 테스트 실행
    run_test "comparison"

    # 결과 분석
    analyze_results

    echo -e "${GREEN}=== 테스트 완료 ===${NC}"
    echo "테스트 종료 시간: $(date)"
}

# 도움말
show_help() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  -h, --help     이 도움말 표시"
    echo "  --dry-run      실제 테스트 없이 사전 확인만 수행"
    echo "  --clean        이전 결과 파일 정리"
    echo ""
    echo "예시:"
    echo "  $0                # 기본 테스트 실행"
    echo "  $0 --dry-run      # 사전 확인만"
    echo "  $0 --clean        # 결과 파일 정리"
}

# 결과 파일 정리
clean_results() {
    echo -e "${YELLOW}이전 테스트 결과를 정리합니다...${NC}"

    if [ -d "$OUTPUT_DIR" ]; then
        rm -rf "$OUTPUT_DIR"/*
        echo -e "${GREEN}✅ 결과 파일 정리 완료${NC}"
    else
        echo "정리할 파일이 없습니다."
    fi
}

# 명령줄 인수 처리
case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    --dry-run)
        echo "Dry run 모드: 사전 확인만 수행합니다."
        prereq_check
        echo -e "${GREEN}✅ 모든 사전 요구사항이 만족됩니다.${NC}"
        exit 0
        ;;
    --clean)
        clean_results
        exit 0
        ;;
    "")
        main
        ;;
    *)
        echo -e "${RED}알 수 없는 옵션: $1${NC}"
        echo "도움말을 보려면 -h 또는 --help를 사용하세요."
        exit 1
        ;;
esac