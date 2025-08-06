#!/bin/bash

# 프로덕션 빌드 스크립트
# 패치 버전을 자동으로 증가시키고 ROOT.war 파일을 생성합니다.

echo "=== Windmill 프로덕션 빌드 시작 ==="

# 현재 버전 정보 읽기
CURRENT_VERSION=$(grep -o '<version>.*</version>' pom.xml | head -1 | sed 's/<version>\(.*\)<\/version>/\1/')
echo "현재 버전: $CURRENT_VERSION"

# 프로덕션 환경 경로 설정 확인
echo "프로덕션 환경 경로: /mobis/DEX/MENU"

# 버전 파싱 (major.minor.patch 형식)
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR=${VERSION_PARTS[0]}
MINOR=${VERSION_PARTS[1]}
PATCH=${VERSION_PARTS[2]}

# 패치 버전 증가
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"

echo "새 버전: $NEW_VERSION"

# 사용자 확인
echo ""
read -p "버전을 $CURRENT_VERSION에서 $NEW_VERSION으로 증가시키시겠습니까? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "버전 증가가 취소되었습니다."
    echo "현재 버전 $CURRENT_VERSION으로 빌드를 계속합니다."
    NEW_VERSION=$CURRENT_VERSION
else
    echo "버전 증가가 확인되었습니다."
    
    # pom.xml의 버전 업데이트
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/" pom.xml
    else
        # Linux
        sed -i "s/<version>$CURRENT_VERSION<\/version>/<version>$NEW_VERSION<\/version>/" pom.xml
    fi
    
    echo "pom.xml 버전 업데이트 완료"
fi

# Maven 클린 및 패키지 (production 프로파일 사용)
echo "Maven 빌드 시작..."
mvn clean package -Pproduction -DskipTests

if [ $? -eq 0 ]; then
    echo "Maven 빌드 성공"
    
    # ROOT.war 파일 복사
    cp target/Windmill-$NEW_VERSION.war target/ROOT.war
    echo "ROOT.war 파일 생성 완료: target/ROOT.war"
    
    echo "=== 빌드 완료 ==="
    echo "버전: $NEW_VERSION"
    echo "WAR 파일: target/ROOT.war"
    echo "환경: 프로덕션 (production 프로파일)"
    echo ""
    echo "배포 방법:"
    echo "1. target/ROOT.war 파일을 Tomcat webapps 디렉토리에 복사"
    echo "2. Tomcat 재시작"
else
    echo "Maven 빌드 실패"
    exit 1
fi 