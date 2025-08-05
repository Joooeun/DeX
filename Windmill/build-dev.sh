#!/bin/bash

# 개발용 빌드 스크립트
# 버전을 증가시키지 않고 빌드합니다.

echo "=== Windmill 개발 빌드 시작 ==="

# 현재 버전 정보 읽기
CURRENT_VERSION=$(grep -o '<version>.*</version>' pom.xml | head -1 | sed 's/<version>\(.*\)<\/version>/\1/')
echo "현재 버전: $CURRENT_VERSION"

# Maven 클린 및 패키지
echo "Maven 빌드 시작..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "Maven 빌드 성공"
    
    # ROOT.war 파일 복사
    cp target/Windmill-$CURRENT_VERSION.war target/ROOT.war
    echo "ROOT.war 파일 생성 완료: target/ROOT.war"
    
    echo "=== 빌드 완료 ==="
    echo "버전: $CURRENT_VERSION"
    echo "WAR 파일: target/ROOT.war"
else
    echo "Maven 빌드 실패"
    exit 1
fi 