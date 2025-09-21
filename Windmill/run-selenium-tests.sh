#!/bin/bash

# Java 1.8 환경 설정
export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
export JRE_HOME=$JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH

# Selenium 테스트 실행 스크립트
# Java 1.8 환경에서 실행

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 디렉토리로 이동
cd "$(dirname "$0")"

echo -e "${BLUE}=== Windmill Selenium 테스트 실행 스크립트 ===${NC}"
echo ""

# Java 1.8 설정
export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo -e "${YELLOW}Java 버전 확인:${NC}"
java -version
echo ""

# Maven 버전 확인
echo -e "${YELLOW}Maven 버전 확인:${NC}"
mvn -version
echo ""

# Chrome 드라이버 확인
echo -e "${YELLOW}Chrome 드라이버 확인:${NC}"
if command -v chromedriver &> /dev/null; then
    chromedriver --version
else
    echo -e "${RED}Chrome 드라이버가 설치되지 않았습니다.${NC}"
    echo "다음 명령으로 설치하세요:"
    echo "brew install chromedriver"
    exit 1
fi
echo ""

# 서버 상태 확인 함수
check_server_status() {
    local url="http://localhost:8080"
    local max_attempts=3
    local attempt=1
    
    echo -e "${YELLOW}서버 상태 확인 중...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s --connect-timeout 5 "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 서버가 실행 중입니다. ($url)${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️ 서버 연결 시도 $attempt/$max_attempts 실패${NC}"
            attempt=$((attempt + 1))
            sleep 2
        fi
    done
    
    echo -e "${RED}❌ 서버가 실행되지 않았습니다.${NC}"
    return 1
}

# 서버 시작 함수
start_server() {
    echo -e "${YELLOW}서버를 시작합니다...${NC}"
    
    # WAR 파일 빌드
    echo -e "${YELLOW}WAR 파일 빌드 중...${NC}"
    JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ 빌드 실패${NC}"
        return 1
    fi
    
    # Tomcat 디렉토리 확인
    local tomcat_webapps="/opt/homebrew/Cellar/tomcat@9/9.0.107/libexec/webapps"
    local war_file="target/Windmill-2.2.11.war"
    local root_war="$tomcat_webapps/ROOT.war"
    
    if [ ! -d "$tomcat_webapps" ]; then
        echo -e "${RED}❌ Tomcat webapps 디렉토리를 찾을 수 없습니다: $tomcat_webapps${NC}"
        return 1
    fi
    
    if [ ! -f "$war_file" ]; then
        echo -e "${RED}❌ WAR 파일을 찾을 수 없습니다: $war_file${NC}"
        return 1
    fi
    
    # 기존 ROOT.war 백업
    if [ -f "$root_war" ]; then
        echo -e "${YELLOW}기존 ROOT.war 백업 중...${NC}"
        mv "$root_war" "$root_war.backup.$(date +%Y%m%d_%H%M%S)"
    fi
    
    # 새 WAR 파일 복사
    echo -e "${YELLOW}WAR 파일 배포 중...${NC}"
    cp "$war_file" "$root_war"
    
    # Tomcat 시작 (Java 1.8 설정)
    echo -e "${YELLOW}Tomcat 시작 중... (Java 1.8)${NC}"
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
    export JRE_HOME=$JAVA_HOME
    /opt/homebrew/Cellar/tomcat@9/9.0.107/libexec/bin/startup.sh
    
    # 서버 시작 대기
    echo -e "${YELLOW}서버 시작 대기 중...${NC}"
    local wait_time=0
    local max_wait=60
    
    while [ $wait_time -lt $max_wait ]; do
        if check_server_status > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 서버가 성공적으로 시작되었습니다.${NC}"
            return 0
        fi
        sleep 3
        wait_time=$((wait_time + 3))
        echo -e "${YELLOW}대기 중... (${wait_time}s/${max_wait}s)${NC}"
    done
    
    echo -e "${RED}❌ 서버 시작 시간 초과${NC}"
    return 1
}

# 서버 중지 함수
stop_server() {
    echo -e "${YELLOW}서버를 중지합니다... (Java 1.8)${NC}"
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
    export JRE_HOME=$JAVA_HOME
    /opt/homebrew/Cellar/tomcat@9/9.0.107/libexec/bin/shutdown.sh
    sleep 5
    echo -e "${GREEN}✅ 서버가 중지되었습니다.${NC}"
}

# 테스트 실행 함수
run_tests() {
    local test_type=$1
    local test_class=$2
    
    echo -e "${BLUE}=== $test_type 테스트 실행 ===${NC}"
    
    # 테스트 실행 시간 기록
    local start_time=$(date +%s)
    
    if [ -n "$test_class" ]; then
        JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home mvn test -Dtest="$test_class" -Dmaven.test.failure.ignore=true
    else
        JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home mvn test -Dmaven.test.failure.ignore=true
    fi
    
    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # 결과 리포트 생성
    generate_test_report "$test_type" "$test_class" "$exit_code" "$duration"
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✅ $test_type 테스트 완료 (${duration}초)${NC}"
    else
        echo -e "${RED}❌ $test_type 테스트 실패 (${duration}초) - 일부 테스트 실패 가능${NC}"
    fi
    
    echo ""
    return $exit_code
}

# 테스트 리포트 생성 함수
generate_test_report() {
    local test_type=$1
    local test_class=$2
    local exit_code=$3
    local duration=$4
    
    # 리포트 디렉토리 생성
    local report_dir="test-reports"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local report_file="$report_dir/test-report-$timestamp.html"
    
    mkdir -p "$report_dir"
    
    # Maven Surefire 리포트 디렉토리
    local surefire_dir="target/surefire-reports"
    
    echo -e "${YELLOW}📊 테스트 리포트 생성 중...${NC}"
    
    # HTML 리포트 생성
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Windmill Selenium 테스트 리포트 - $(date '+%Y-%m-%d %H:%M:%S')</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0; }
        .header h1 { margin: 0; font-size: 2.5em; }
        .header p { margin: 10px 0 0 0; opacity: 0.9; }
        .summary { padding: 30px; border-bottom: 1px solid #eee; }
        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px; }
        .summary-card { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; border-left: 4px solid #007bff; }
        .summary-card.success { border-left-color: #28a745; }
        .summary-card.failure { border-left-color: #dc3545; }
        .summary-card h3 { margin: 0 0 10px 0; color: #333; }
        .summary-card .number { font-size: 2em; font-weight: bold; margin: 10px 0; }
        .summary-card.success .number { color: #28a745; }
        .summary-card.failure .number { color: #dc3545; }
        .details { padding: 30px; }
        .test-class { margin-bottom: 30px; border: 1px solid #ddd; border-radius: 8px; overflow: hidden; }
        .test-class-header { background: #f8f9fa; padding: 15px 20px; border-bottom: 1px solid #ddd; font-weight: bold; }
        .test-class.success .test-class-header { background: #d4edda; color: #155724; }
        .test-class.failure .test-class-header { background: #f8d7da; color: #721c24; }
        .test-method { padding: 15px 20px; border-bottom: 1px solid #eee; }
        .test-method:last-child { border-bottom: none; }
        .test-method.success { background: #f8fff9; }
        .test-method.failure { background: #fff8f8; }
        .test-name { font-weight: bold; margin-bottom: 5px; }
        .test-duration { color: #666; font-size: 0.9em; }
        .test-error { background: #f8f9fa; padding: 10px; border-radius: 4px; margin-top: 10px; font-family: monospace; font-size: 0.9em; color: #dc3545; }
        .footer { padding: 20px 30px; background: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center; color: #666; }
        .status-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 0.8em; font-weight: bold; text-transform: uppercase; }
        .status-success { background: #d4edda; color: #155724; }
        .status-failure { background: #f8d7da; color: #721c24; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🧪 Windmill Selenium 테스트 리포트</h1>
            <p>실행 시간: $(date '+%Y-%m-%d %H:%M:%S') | 테스트 유형: $test_type | 소요 시간: ${duration}초</p>
        </div>
        
        <div class="summary">
            <h2>📈 테스트 요약</h2>
            <div class="summary-grid">
EOF

    # 테스트 결과 통계 계산
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    local total_duration=0
    
    if [ -d "$surefire_dir" ]; then
        # XML 파일에서 테스트 결과 파싱
        for xml_file in "$surefire_dir"/*.xml; do
            if [ -f "$xml_file" ]; then
                local tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | grep -o '[0-9]*' | head -1)
                local failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | grep -o '[0-9]*' | head -1)
                local errors=$(grep -o 'errors="[0-9]*"' "$xml_file" | grep -o '[0-9]*' | head -1)
                local time=$(grep -o 'time="[0-9.]*"' "$xml_file" | grep -o '[0-9.]*' | head -1)
                
                total_tests=$((total_tests + ${tests:-0}))
                failed_tests=$((failed_tests + ${failures:-0} + ${errors:-0}))
                total_duration=$(echo "$total_duration + ${time:-0}" | bc -l 2>/dev/null || echo "$total_duration")
            fi
        done
    fi
    
    passed_tests=$((total_tests - failed_tests))
    
    # 요약 카드 추가
    cat >> "$report_file" << EOF
                <div class="summary-card">
                    <h3>총 테스트</h3>
                    <div class="number">$total_tests</div>
                </div>
                <div class="summary-card success">
                    <h3>성공</h3>
                    <div class="number">$passed_tests</div>
                </div>
                <div class="summary-card failure">
                    <h3>실패</h3>
                    <div class="number">$failed_tests</div>
                </div>
                <div class="summary-card">
                    <h3>소요 시간</h3>
                    <div class="number">${duration}초</div>
                </div>
            </div>
        </div>
        
        <div class="details">
            <h2>📋 상세 결과</h2>
EOF

    # 각 테스트 클래스별 상세 결과 추가
    if [ -d "$surefire_dir" ]; then
        for xml_file in "$surefire_dir"/*.xml; do
            if [ -f "$xml_file" ]; then
                local class_name=$(basename "$xml_file" .xml)
                local tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | grep -o '[0-9]*' | head -1)
                local failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | grep -o '[0-9]*' | head -1)
                local errors=$(grep -o 'errors="[0-9]*"' "$xml_file" | grep -o '[0-9]*' | head -1)
                local time=$(grep -o 'time="[0-9.]*"' "$xml_file" | grep -o '[0-9.]*' | head -1)
                
                local class_status="success"
                if [ $((failures + errors)) -gt 0 ]; then
                    class_status="failure"
                fi
                
                cat >> "$report_file" << EOF
            <div class="test-class $class_status">
                <div class="test-class-header">
                    <span class="status-badge status-$class_status">$class_status</span>
                    $class_name
                    <span style="float: right;">테스트: ${tests:-0} | 실패: $((failures + errors)) | 시간: ${time:-0}초</span>
                </div>
EOF

                # 개별 테스트 메서드 결과 추가
                if [ -f "$xml_file" ]; then
                    # XML에서 테스트 케이스 추출 (간단한 파싱)
                    grep -o '<testcase[^>]*>' "$xml_file" | while read -r testcase; do
                        local test_name=$(echo "$testcase" | grep -o 'name="[^"]*"' | sed 's/name="//;s/"//')
                        local test_time=$(echo "$testcase" | grep -o 'time="[^"]*"' | sed 's/time="//;s/"//')
                        
                        if [ -n "$test_name" ]; then
                            cat >> "$report_file" << EOF
                <div class="test-method success">
                    <div class="test-name">✅ $test_name</div>
                    <div class="test-duration">소요 시간: ${test_time:-0}초</div>
                </div>
EOF
                        fi
                    done
                    
                    # 실패한 테스트 케이스 추가
                    grep -A 5 '<failure\|<error' "$xml_file" | while read -r line; do
                        if echo "$line" | grep -q '<testcase'; then
                            local test_name=$(echo "$line" | grep -o 'name="[^"]*"' | sed 's/name="//;s/"//')
                            if [ -n "$test_name" ]; then
                                cat >> "$report_file" << EOF
                <div class="test-method failure">
                    <div class="test-name">❌ $test_name</div>
                    <div class="test-duration">실패</div>
                </div>
EOF
                            fi
                        fi
                    done
                fi
                
                cat >> "$report_file" << EOF
            </div>
EOF
            fi
        done
    fi
    
    # 리포트 마무리
    cat >> "$report_file" << EOF
        </div>
        
        <div class="footer">
            <p>Windmill Selenium 테스트 자동화 | 생성 시간: $(date '+%Y-%m-%d %H:%M:%S')</p>
            <p>Java 버전: $(java -version 2>&1 | head -1) | Maven 버전: $(mvn -version 2>&1 | head -1)</p>
        </div>
    </div>
</body>
</html>
EOF

    echo -e "${GREEN}📊 리포트 생성 완료: $report_file${NC}"
    
    # 최신 리포트 링크 생성
    ln -sf "$report_file" "$report_dir/latest-report.html"
    echo -e "${BLUE}🔗 최신 리포트: $report_dir/latest-report.html${NC}"
    
    # 간단한 콘솔 요약 출력
    echo -e "${YELLOW}📊 테스트 결과 요약:${NC}"
    echo -e "  총 테스트: $total_tests"
    echo -e "  성공: ${GREEN}$passed_tests${NC}"
    echo -e "  실패: ${RED}$failed_tests${NC}"
    echo -e "  소요 시간: ${duration}초"
    
    if [ $failed_tests -gt 0 ]; then
        echo -e "${RED}❌ 일부 테스트가 실패했습니다. 상세 내용은 리포트를 확인하세요.${NC}"
    else
        echo -e "${GREEN}✅ 모든 테스트가 성공했습니다!${NC}"
    fi
}

# 메인 실행 로직
case "${1:-all}" in
    "all")
        echo -e "${YELLOW}전체 테스트 실행${NC}"
        # 서버 상태 확인 및 시작
        if ! check_server_status; then
            if ! start_server; then
                echo -e "${RED}❌ 서버 시작 실패로 테스트를 중단합니다.${NC}"
                exit 1
            fi
        fi
        run_tests "전체" ""
        ;;
    "sql")
        echo -e "${YELLOW}SQL 템플릿 테스트 실행${NC}"
        # 서버 상태 확인 및 시작
        if ! check_server_status; then
            if ! start_server; then
                echo -e "${RED}❌ 서버 시작 실패로 테스트를 중단합니다.${NC}"
                exit 1
            fi
        fi
        run_tests "SQL 템플릿" "SqlTemplateBugTests"
        ;;
    "connection")
        echo -e "${YELLOW}연결 관리 테스트 실행${NC}"
        # 서버 상태 확인 및 시작
        if ! check_server_status; then
            if ! start_server; then
                echo -e "${RED}❌ 서버 시작 실패로 테스트를 중단합니다.${NC}"
                exit 1
            fi
        fi
        run_tests "연결 관리" "ConnectionBugTests"
        ;;
    "dashboard")
        echo -e "${YELLOW}대시보드 테스트 실행${NC}"
        # 서버 상태 확인 및 시작
        if ! check_server_status; then
            if ! start_server; then
                echo -e "${RED}❌ 서버 시작 실패로 테스트를 중단합니다.${NC}"
                exit 1
            fi
        fi
        run_tests "대시보드" "DashboardBugTests"
        ;;
    "start")
        echo -e "${YELLOW}서버 시작${NC}"
        if check_server_status; then
            echo -e "${GREEN}✅ 서버가 이미 실행 중입니다.${NC}"
        else
            start_server
        fi
        ;;
    "stop")
        echo -e "${YELLOW}서버 중지${NC}"
        stop_server
        ;;
    "restart")
        echo -e "${YELLOW}서버 재시작${NC}"
        stop_server
        sleep 3
        start_server
        ;;
    "status")
        echo -e "${YELLOW}서버 상태 확인${NC}"
        check_server_status
        ;;
    "compile")
        echo -e "${YELLOW}테스트 컴파일만 실행${NC}"
        JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home mvn test-compile
        echo -e "${GREEN}✅ 테스트 컴파일 완료${NC}"
        ;;
    "clean")
        echo -e "${YELLOW}프로젝트 정리${NC}"
        JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home mvn clean
        echo -e "${GREEN}✅ 프로젝트 정리 완료${NC}"
        ;;
    "report")
        echo -e "${YELLOW}최신 테스트 리포트 열기${NC}"
        if [ -f "test-reports/latest-report.html" ]; then
            echo -e "${GREEN}📊 리포트를 브라우저에서 열고 있습니다...${NC}"
            open "test-reports/latest-report.html" 2>/dev/null || xdg-open "test-reports/latest-report.html" 2>/dev/null || echo -e "${YELLOW}브라우저에서 수동으로 열어주세요: test-reports/latest-report.html${NC}"
        else
            echo -e "${RED}❌ 리포트 파일을 찾을 수 없습니다. 먼저 테스트를 실행해주세요.${NC}"
        fi
        ;;
    "reports")
        echo -e "${YELLOW}모든 테스트 리포트 목록${NC}"
        if [ -d "test-reports" ]; then
            echo -e "${BLUE}📁 test-reports 디렉토리:${NC}"
            ls -la test-reports/*.html 2>/dev/null | while read -r line; do
                echo "  $line"
            done
            echo ""
            echo -e "${GREEN}최신 리포트: test-reports/latest-report.html${NC}"
        else
            echo -e "${RED}❌ test-reports 디렉토리가 없습니다. 먼저 테스트를 실행해주세요.${NC}"
        fi
        ;;
    "help"|"-h"|"--help")
        echo "사용법: $0 [옵션]"
        echo ""
        echo "옵션:"
        echo "  all        전체 테스트 실행 (기본값)"
        echo "  sql        SQL 템플릿 테스트만 실행"
        echo "  connection 연결 관리 테스트만 실행"
        echo "  dashboard  대시보드 테스트만 실행"
        echo "  start      서버 시작"
        echo "  stop       서버 중지"
        echo "  restart    서버 재시작"
        echo "  status     서버 상태 확인"
        echo "  compile    테스트 컴파일만 실행"
        echo "  clean      프로젝트 정리"
        echo "  report     최신 테스트 리포트 열기"
        echo "  reports    모든 테스트 리포트 목록 보기"
        echo "  help       도움말 표시"
        echo ""
        echo "예시:"
        echo "  $0                    # 전체 테스트 실행 (서버 자동 시작)"
        echo "  $0 sql               # SQL 템플릿 테스트만 실행"
        echo "  $0 start             # 서버만 시작"
        echo "  $0 status            # 서버 상태 확인"
        echo "  $0 compile           # 컴파일만 실행"
        echo "  $0 report            # 최신 테스트 리포트 열기"
        echo "  $0 reports           # 모든 리포트 목록 보기"
        ;;
    *)
        echo -e "${RED}알 수 없는 옵션: $1${NC}"
        echo "도움말을 보려면: $0 help"
        exit 1
        ;;
esac

echo -e "${GREEN}=== 테스트 실행 완료 ===${NC}"
