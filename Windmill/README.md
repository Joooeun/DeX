# Windmill 프로젝트

## 버전 관리 시스템

이 프로젝트는 자동화된 버전 관리 시스템을 사용합니다.

### 현재 버전
- **버전**: 2.2.1

### 빌드 방법

#### 1. 개발용 빌드 (버전 증가 없음)
```bash
./build-dev.sh
```

#### 2. 프로덕션 빌드 (패치 버전 자동 증가, 사용자 확인)
```bash
./build-production.sh
```

### 버전 표시

애플리케이션의 footer에서 현재 버전 정보를 확인할 수 있습니다:
- 버전 번호

### 배포 방법

1. 빌드 스크립트 실행
2. `target/ROOT.war` 파일을 Tomcat webapps 디렉토리에 복사
3. Tomcat 재시작

### 버전 형식

- **Major.Minor.Patch** 형식 사용
- 프로덕션 빌드 시 Patch 버전이 자동으로 1씩 증가 (사용자 확인 후)
- 예: 2.2.1 → 2.2.2 → 2.2.3

### 파일 구조

- `pom.xml`: Maven 프로젝트 설정 및 버전 정보
- `src/main/resources/version.properties`: 버전 프로퍼티 파일
- `src/main/java/kr/Windmill/util/VersionUtil.java`: 버전 정보 유틸리티
- `src/main/webapp/WEB-INF/views/common/footer.jsp`: 버전 표시
- `build-production.sh`: 프로덕션 빌드 스크립트
- `build-dev.sh`: 개발용 빌드 스크립트 