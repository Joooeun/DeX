#!/bin/bash

# 전체 테스트 실행 스크립트
# API 테스트와 브라우저 자동화 테스트를 모두 실행하고 결과를 종합합니다.

echo "=========================================="
echo "🚀 전체 테스트 실행 시작"
echo "=========================================="

# 현재 시간 기록
START_TIME=$(date)
echo "테스트 시작 시간: $START_TIME"

# 결과 디렉토리 생성
mkdir -p test-results
mkdir -p screenshots
mkdir -p test-reports

# 1. API 테스트 실행
echo ""
echo "=========================================="
echo "📡 1단계: API 테스트 실행"
echo "=========================================="

# API 테스트 스크립트가 있다면 실행
if [ -f "run-api-tests.sh" ]; then
    echo "API 테스트 스크립트 실행 중..."
    ./run-api-tests.sh > test-results/api-test-results.txt 2>&1
    API_RESULT=$?
    echo "API 테스트 결과: $API_RESULT"
else
    echo "API 테스트 스크립트가 없습니다. 건너뜁니다."
    API_RESULT=0
fi

# 2. 브라우저 자동화 테스트 실행
echo ""
echo "=========================================="
echo "🌐 2단계: 브라우저 자동화 테스트 실행"
echo "=========================================="

echo "브라우저 자동화 테스트 실행 중..."
./run-browser-tests.sh > test-results/browser-test-results.txt 2>&1
BROWSER_RESULT=$?
echo "브라우저 테스트 결과: $BROWSER_RESULT"

# 3. 테스트 결과 리포트 생성
echo ""
echo "=========================================="
echo "📊 3단계: 테스트 결과 리포트 생성"
echo "=========================================="

echo "HTML 리포트 생성 중..."
./generate-test-report.sh

# 4. 종합 결과 분석
echo ""
echo "=========================================="
echo "📈 4단계: 종합 결과 분석"
echo "=========================================="

# 테스트 완료 시간 기록
END_TIME=$(date)

# 전체 결과 계산
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# API 테스트 결과 분석
if [ $API_RESULT -eq 0 ]; then
    echo "✅ API 테스트: 성공"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "❌ API 테스트: 실패"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

# 브라우저 테스트 결과 분석
if [ $BROWSER_RESULT -eq 0 ]; then
    echo "✅ 브라우저 테스트: 성공"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "❌ 브라우저 테스트: 실패"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

# 성공률 계산
SUCCESS_RATE=0
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
fi

# 5. 최종 결과 출력
echo ""
echo "=========================================="
echo "🎯 최종 테스트 결과"
echo "=========================================="

echo "📊 테스트 통계:"
echo "   총 테스트 수: $TOTAL_TESTS"
echo "   성공한 테스트: $PASSED_TESTS"
echo "   실패한 테스트: $FAILED_TESTS"
echo "   성공률: $SUCCESS_RATE%"

echo ""
echo "⏰ 시간 정보:"
echo "   시작 시간: $START_TIME"
echo "   완료 시간: $END_TIME"

echo ""
echo "📁 생성된 파일들:"
echo "   - test-results/api-test-results.txt (API 테스트 결과)"
echo "   - test-results/browser-test-results.txt (브라우저 테스트 결과)"
echo "   - test-reports/browser-test-report.html (HTML 리포트)"
if [ -d "screenshots" ] && [ "$(ls -A screenshots)" ]; then
    echo "   - screenshots/ (테스트 스크린샷)"
fi

echo ""
echo "🌐 리포트 보기:"
echo "   open test-reports/browser-test-report.html"

# 6. 종합 결과에 따른 종료 코드 설정
if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo "🎉 모든 테스트가 성공적으로 완료되었습니다!"
    FINAL_RESULT=0
else
    echo ""
    echo "⚠️  일부 테스트가 실패했습니다. 자세한 내용은 위의 로그를 확인하세요."
    FINAL_RESULT=1
fi

echo "=========================================="

exit $FINAL_RESULT
