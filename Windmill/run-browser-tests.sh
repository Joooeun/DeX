#!/bin/bash

# 브라우저 자동화 테스트 실행 스크립트
# Selenium WebDriver를 사용하여 웹 UI 기능을 자동으로 테스트합니다.

echo "=========================================="
echo "브라우저 자동화 테스트 시작"
echo "=========================================="

# 현재 시간 기록
START_TIME=$(date)
echo "테스트 시작 시간: $START_TIME"

# 스크린샷 디렉토리 생성
mkdir -p screenshots

# Maven 의존성 다운로드 및 컴파일
echo "Maven 의존성 다운로드 및 컴파일 중..."
mvn clean compile test-compile

if [ $? -ne 0 ]; then
    echo "❌ Maven 컴파일 실패"
    exit 1
fi

echo "✅ Maven 컴파일 완료"

# 톰캣 서버 상태 확인
echo "톰캣 서버 상태 확인 중..."
if ! curl -s http://localhost:8080 > /dev/null; then
    echo "❌ 톰캣 서버가 실행되지 않았습니다. 서버를 시작해주세요."
    exit 1
fi

echo "✅ 톰캣 서버 실행 중 확인"

# 브라우저 자동화 테스트 실행
echo "브라우저 자동화 테스트 실행 중..."
mvn test -Dtest=BrowserAutomationTest

# 테스트 결과 확인
TEST_RESULT=$?

# 테스트 완료 시간 기록
END_TIME=$(date)
echo "테스트 완료 시간: $END_TIME"

echo "=========================================="
echo "테스트 결과 요약"
echo "=========================================="

if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ 모든 테스트가 성공적으로 완료되었습니다!"
    echo ""
    echo "실행된 테스트:"
    echo "- 로그인 테스트"
    echo "- SQL 템플릿 관리 페이지 접근 테스트"
    echo "- SQL 템플릿 상세 조회 테스트"
    echo "- 새 템플릿 생성 테스트"
    echo "- 대시보드 페이지 접근 테스트"
    echo "- 연결 관리 페이지 접근 테스트"
else
    echo "❌ 일부 테스트가 실패했습니다."
    echo "자세한 내용은 위의 로그를 확인하세요."
    echo ""
    echo "실패한 테스트의 스크린샷이 screenshots/ 디렉토리에 저장되었습니다."
fi

echo ""
echo "테스트 시작: $START_TIME"
echo "테스트 완료: $END_TIME"
echo "=========================================="

exit $TEST_RESULT
