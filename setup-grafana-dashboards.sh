#!/bin/bash

# Grafana 대시보드 자동 설정 스크립트
# MySQL, Keycloak, Spring Boot, JVM 메트릭 대시보드 구성

set -e

GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASSWORD="${GRAFANA_PASSWORD:-admin}"

echo "🚀 Grafana 대시보드 설정 시작..."

# Grafana가 준비될 때까지 대기
wait_for_grafana() {
    echo "⏳ Grafana 시작 대기 중..."
    for i in {1..30}; do
        if curl -s "${GRAFANA_URL}/api/health" > /dev/null 2>&1; then
            echo "✅ Grafana가 준비되었습니다."
            return 0
        fi
        sleep 2
    done
    echo "❌ Grafana 연결 실패"
    exit 1
}

# 데이터소스 추가
add_datasource() {
    local name=$1
    local type=$2
    local url=$3
    local database=$4
    
    echo "📊 데이터소스 추가: ${name}"
    
    cat <<EOF | curl -X POST \
        -H "Content-Type: application/json" \
        -u "${GRAFANA_USER}:${GRAFANA_PASSWORD}" \
        "${GRAFANA_URL}/api/datasources" \
        -d @-
{
  "name": "${name}",
  "type": "${type}",
  "url": "${url}",
  "access": "proxy",
  "database": "${database}",
  "basicAuth": false,
  "isDefault": $([ "$name" == "Prometheus" ] && echo "true" || echo "false"),
  "jsonData": {
    "timeInterval": "5s"
  }
}
EOF
}

# 대시보드 임포트
import_dashboard() {
    local dashboard_id=$1
    local datasource_name=$2
    local title=$3
    
    echo "📈 대시보드 임포트: ${title} (ID: ${dashboard_id})"
    
    cat <<EOF | curl -X POST \
        -H "Content-Type: application/json" \
        -u "${GRAFANA_USER}:${GRAFANA_PASSWORD}" \
        "${GRAFANA_URL}/api/dashboards/import" \
        -d @-
{
  "dashboard": {
    "id": ${dashboard_id}
  },
  "overwrite": true,
  "inputs": [
    {
      "name": "DS_PROMETHEUS",
      "type": "datasource",
      "pluginId": "prometheus",
      "value": "${datasource_name}"
    }
  ],
  "folderId": 0
}
EOF
}

# 커스텀 대시보드 생성
create_custom_dashboard() {
    echo "🎨 커스텀 이벤트 참여 대시보드 생성..."
    
    cat <<'EOF' | curl -X POST \
        -H "Content-Type: application/json" \
        -u "${GRAFANA_USER}:${GRAFANA_PASSWORD}" \
        "${GRAFANA_URL}/api/dashboards/db" \
        -d @-
{
  "dashboard": {
    "title": "Event Participation Monitoring",
    "panels": [
      {
        "id": 1,
        "title": "Request Rate by Lock Strategy",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{endpoint=\"participate\"}[1m])) by (strategy)",
            "legendFormat": "{{strategy}}"
          }
        ]
      },
      {
        "id": 2,
        "title": "Success Rate",
        "type": "gauge",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0},
        "targets": [
          {
            "expr": "sum(rate(participation_success_total[5m])) / sum(rate(participation_attempts_total[5m])) * 100"
          }
        ]
      },
      {
        "id": 3,
        "title": "Response Time P95",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{endpoint=\"participate\"}[1m]))",
            "legendFormat": "p95"
          }
        ]
      },
      {
        "id": 4,
        "title": "Database Connections",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8},
        "targets": [
          {
            "expr": "hikaricp_connections_active{pool=\"HikariPool-1\"}",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle{pool=\"HikariPool-1\"}",
            "legendFormat": "Idle"
          }
        ]
      },
      {
        "id": 5,
        "title": "JVM Memory Usage",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16},
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / 1024 / 1024",
            "legendFormat": "Heap Used (MB)"
          }
        ]
      },
      {
        "id": 6,
        "title": "Error Rate by Type",
        "type": "graph",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 16},
        "targets": [
          {
            "expr": "sum(rate(participation_errors_total[1m])) by (error_type)",
            "legendFormat": "{{error_type}}"
          }
        ]
      }
    ],
    "refresh": "5s",
    "time": {
      "from": "now-30m",
      "to": "now"
    }
  },
  "overwrite": true
}
EOF
}

# 메인 실행
main() {
    wait_for_grafana
    
    # 데이터소스 설정
    add_datasource "Prometheus" "prometheus" "http://prometheus:9090" ""
    add_datasource "MySQL" "mysql" "mysql:3306" "booker"
    
    # 표준 대시보드 임포트
    import_dashboard 7362 "Prometheus" "MySQL Overview"           # MySQL 대시보드
    import_dashboard 4701 "Prometheus" "JVM Micrometer"          # JVM 메트릭
    import_dashboard 11378 "Prometheus" "Spring Boot Statistics"  # Spring Boot
    import_dashboard 12900 "Prometheus" "K6 Load Testing"        # K6 메트릭
    
    # 커스텀 대시보드 생성
    create_custom_dashboard
    
    echo "✅ Grafana 대시보드 설정 완료!"
    echo "🔗 접속 URL: ${GRAFANA_URL}"
    echo "👤 계정: ${GRAFANA_USER} / ${GRAFANA_PASSWORD}"
}

main "$@"