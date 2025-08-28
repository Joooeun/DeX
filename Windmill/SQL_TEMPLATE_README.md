# SQL 템플릿 관리 기능

## 개요
기존 AdminLTE + Bootstrap3 + jQuery + Ace Editor 기반 프로젝트에 추가된 SQL 템플릿 관리 기능입니다.

## 주요 기능

### 1. 카테고리 관리
- 카테고리 생성, 수정, 삭제
- 카테고리 순서 변경 (위/아래 버튼)
- 카테고리별 템플릿 개수 표시

### 2. 템플릿 관리
- SQL 템플릿 생성, 수정, 삭제
- 템플릿명, 설명, 상태, 실행제한, 새로고침 타임아웃 설정
- Ace Editor를 통한 SQL 편집 (theme=chrome, mode=sql)
- 템플릿 검색 및 상태 필터링

### 3. 드래그 앤 드롭
- 템플릿을 카테고리에 드래그하여 매핑
- ALL/UNASSIGNED 카테고리는 드롭 금지

### 4. 데이터베이스 기반
- JdbcTemplate을 사용한 DB 연동
- 트랜잭션 적용
- 소프트 삭제 (status = 'DELETED')

### 5. 데이터베이스 연결 관리
- 다중 데이터베이스 연결 지원 (MySQL, Oracle, PostgreSQL, SQL Server 등)
- 동적 커넥션 풀 관리
- 연결 상태 실시간 모니터링
- 연결 테스트 및 검증

### 6. 권한 관리
- 사용자별 데이터베이스 연결 권한 관리
- 그룹 기반 권한 시스템
- 관리자/일반 사용자 권한 분리

## 화면 구성

### 좌측 (2/12): 카테고리 사이드바
- ALL, UNASSIGNED (고정 항목, 드롭 금지)
- DB 카테고리 목록 (정렬 순서대로)
- 각 카테고리별 템플릿 개수 뱃지
- 카테고리별 위/아래/수정/삭제 버튼

### 중앙 (3/12): 템플릿 목록
- 검색 필터 (템플릿명)
- 상태 필터 (ACTIVE/INACTIVE)
- 템플릿 목록 (draggable)
- 각 템플릿별 수정/삭제 버튼

### 우측 (7/12): 템플릿 편집
- 기본 정보 입력 폼
- 카테고리 체크박스
- Ace Editor (SQL 편집)
- 저장/실행 버튼

## API 엔드포인트

### 카테고리 관련
- `GET /SQLTemplate/categories` - 카테고리 목록 조회
- `POST /SQLTemplate/category` - 카테고리 생성
- `PUT /SQLTemplate/category` - 카테고리 수정
- `DELETE /SQLTemplate/category` - 카테고리 삭제
- `POST /SQLTemplate/category/reorder` - 카테고리 순서 변경

### 템플릿 관련
- `GET /SQLTemplate/templates` - 템플릿 목록 조회
- `GET /SQLTemplate/template` - 템플릿 상세 조회
- `POST /SQLTemplate/template` - 템플릿 저장 (생성/수정)
- `DELETE /SQLTemplate/template` - 템플릿 삭제
- `POST /SQLTemplate/template/category` - 템플릿-카테고리 매핑 추가
- `POST /SQLTemplate/template/execute` - SQL 실행 테스트

### 연결 관리 관련
- `GET /connection/list` - 연결 목록 조회 (페이징 지원)
- `GET /connection/detail` - 연결 상세 정보 조회
- `POST /connection/save` - 연결 저장 (생성/수정)
- `DELETE /connection/delete` - 연결 삭제
- `POST /connection/test` - 연결 테스트
- `GET /connection/status` - 연결 상태 조회
- `POST /connection/status/update` - 연결 상태 수동 업데이트

## 데이터베이스 스키마

### sql_template_category
- 카테고리 정보 저장
- 정렬 순서, 상태 관리

### sql_template
- SQL 템플릿 정보 저장
- SQL 내용, 실행 제한, 새로고침 타임아웃

### sql_template_category_mapping
- 템플릿과 카테고리 간 다대다 관계
- 매핑 정보 및 생성 이력

### DATABASE_CONNECTION
- 데이터베이스 연결 정보 저장
- 연결 풀 설정, 모니터링 설정 포함

### SFTP_CONNECTION
- SFTP 연결 정보 저장
- 파일 전송용 연결 관리

### USER_GROUP_MAPPING
- 사용자와 그룹 간 매핑
- 권한 관리 기반

### GROUP_CONNECTION_MAPPING
- 그룹과 연결 간 매핑
- 그룹별 연결 권한 관리

## SQL 작성 지침

### 1. 기본 SQL 작성 규칙

#### 1.1 쿼리 타임아웃 설정
```java
// ConnectionService.java 참조
stmt.setQueryTimeout(5); // 5초 쿼리 타임아웃
```
- 모든 SQL 실행 시 적절한 타임아웃 설정 필수
- 기본값: 5초 (설정 가능)

#### 1.2 연결 풀 사용
```java
// 동적 커넥션 풀에서 연결 가져오기
Connection conn = dynamicJdbcManager.getConnection(connectionId);
```
- 직접 연결 생성 대신 커넥션 풀 사용
- 연결 ID로 특정 데이터베이스 연결 사용

#### 1.3 예외 처리
```java
try {
    // SQL 실행
} catch (SQLException e) {
    logger.error("SQL 실행 실패: {}", e.getMessage(), e);
    // 적절한 오류 응답 반환
} finally {
    // 리소스 정리
    if (conn != null) {
        try {
            conn.close();
        } catch (SQLException e) {
            logger.error("커넥션 닫기 실패", e);
        }
    }
}
```

### 2. 데이터베이스별 SQL 작성

#### 2.1 MySQL/MariaDB
```sql
-- 기본 테스트 쿼리
SELECT 1;

-- 페이징 처리
SELECT * FROM table_name 
LIMIT ? OFFSET ?;

-- 날짜 처리
SELECT DATE_FORMAT(created_date, '%Y-%m-%d %H:%i:%s') FROM table_name;
```

#### 2.2 Oracle
```sql
-- 기본 테스트 쿼리
SELECT 1 FROM DUAL;

-- 페이징 처리
SELECT * FROM (
    SELECT a.*, ROWNUM rnum FROM (
        SELECT * FROM table_name ORDER BY column_name
    ) a WHERE ROWNUM <= ?
) WHERE rnum > ?;

-- 날짜 처리
SELECT TO_CHAR(created_date, 'YYYY-MM-DD HH24:MI:SS') FROM table_name;
```

#### 2.3 PostgreSQL
```sql
-- 기본 테스트 쿼리
SELECT 1;

-- 페이징 처리
SELECT * FROM table_name 
LIMIT ? OFFSET ?;

-- 날짜 처리
SELECT TO_CHAR(created_date, 'YYYY-MM-DD HH24:MI:SS') FROM table_name;
```

#### 2.4 SQL Server
```sql
-- 기본 테스트 쿼리
SELECT 1;

-- 페이징 처리 (SQL Server 2012+)
SELECT * FROM table_name 
ORDER BY column_name
OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;

-- 날짜 처리
SELECT FORMAT(created_date, 'yyyy-MM-dd HH:mm:ss') FROM table_name;
```

### 3. 보안 고려사항

#### 3.1 SQL 인젝션 방지
```java
// PreparedStatement 사용 필수
String sql = "SELECT * FROM users WHERE user_id = ?";
try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setString(1, userId);
    // 실행
}
```

#### 3.2 권한 검증
```java
// 사용자 권한 확인
if (!"admin".equals(userId)) {
    // 권한이 있는 연결만 필터링
    List<String> authorizedConnections = getUserDatabaseConnections(userId);
    // 권한 검증 로직
}
```

#### 3.3 입력값 검증
```java
// SQL 키워드 필터링
if (sql.contains("DROP") || sql.contains("DELETE") || sql.contains("TRUNCATE")) {
    throw new SecurityException("허용되지 않는 SQL 키워드");
}
```

### 4. 성능 최적화

#### 4.1 인덱스 활용
```sql
-- 자주 조회되는 컬럼에 인덱스 생성
CREATE INDEX idx_connection_id ON DATABASE_CONNECTION(CONNECTION_ID);
CREATE INDEX idx_status ON DATABASE_CONNECTION(STATUS);
```

#### 4.2 페이징 처리
```java
// 대용량 데이터 조회 시 페이징 필수
int pageSize = 50;
int offset = (page - 1) * pageSize;
```

#### 4.3 연결 풀 설정
```java
// 적절한 풀 크기 설정
maxPoolSize: 20
minPoolSize: 5
connectionTimeout: 30초
queryTimeout: 5초
```

### 5. 모니터링 및 로깅

#### 5.1 연결 상태 모니터링
```java
// 연결 상태 실시간 모니터링
private void updateConnectionStatus(String connectionId) {
    boolean isConnected = testConnectionWithPool(connectionId);
    // 상태 업데이트 및 로깅
}
```

#### 5.2 로깅 레벨
```java
// 성공한 연결은 DEBUG 레벨
logger.debug("DB 연결 상태 확인 완료: {} - 연결됨", connectionId);

// 실패한 연결은 WARN 레벨
cLog.monitoringLog("CONNECTION_STATUS_WARN", "DB 연결 상태 확인 완료: " + connectionId + " - 연결실패");
```

### 6. 트랜잭션 관리

#### 6.1 트랜잭션 어노테이션
```java
@Transactional
public boolean saveConnection(Map<String, Object> connectionData, String userId) {
    // 트랜잭션 내에서 실행
}
```

#### 6.2 롤백 처리
```java
try {
    // 데이터베이스 작업
} catch (Exception e) {
    // 자동 롤백 (트랜잭션 어노테이션 사용 시)
    throw new RuntimeException("저장 실패", e);
}
```

## 설치 및 실행

### 1. 데이터베이스 설정
```sql
-- SQL_TEMPLATE_SCHEMA.sql 실행
source SQL_TEMPLATE_SCHEMA.sql;

-- COMPLETE_DATABASE_SCHEMA.sql 실행 (전체 스키마)
source COMPLETE_DATABASE_SCHEMA.sql;
```

### 2. 프로젝트 빌드
```bash
cd Windmill
mvn clean package
```

### 3. WAR 파일 배포
```bash
# ROOT.war 파일을 Tomcat webapps 디렉토리에 배포
cp target/ROOT.war /path/to/tomcat/webapps/
```

### 4. 접속
```
http://localhost:8080/SQLTemplateNew
```

## 기술 스택

### 백엔드
- Spring Framework
- JdbcTemplate (MyBatis 대신)
- Java 8+
- MySQL/MariaDB/Oracle/PostgreSQL/SQL Server

### 프론트엔드
- AdminLTE
- Bootstrap 3
- jQuery
- Ace Editor
- HTML5 Drag & Drop API

## 보안

### 권한 관리
- 관리자 권한 확인 (PermissionService.isAdmin())
- 세션 기반 인증
- XSS 방지 (escapeHtml 함수)
- 사용자별 데이터베이스 연결 권한 관리

### 데이터 검증
- SQL 문법 검증
- 입력값 검증
- 트랜잭션 관리
- SQL 인젝션 방지

### 연결 보안
- 연결 정보 암호화 저장
- 연결 테스트 및 검증
- 모니터링을 통한 이상 감지

## 주요 특징

### 1. 기존 스타일 유지
- AdminLTE 박스, Grid, 버튼 스타일 그대로 사용
- 기존 화면과 일관된 UI/UX

### 2. 성능 최적화
- SQL 본문 지연 로드 (리스트에서는 불러오지 않음)
- 인덱스 활용
- 페이징 처리 (필요시 확장 가능)
- 커넥션 풀을 통한 연결 최적화

### 3. 사용자 경험
- 드래그 앤 드롭으로 직관적인 카테고리 매핑
- 실시간 검색 및 필터링
- 모달을 통한 편집 및 실행 결과 표시
- 실시간 연결 상태 모니터링

### 4. 확장성
- 카테고리 순서 변경 기능
- 템플릿 파라미터 지원 (향후 확장)
- 다중 카테고리 매핑 지원
- 다중 데이터베이스 연결 지원

### 5. 모니터링 및 관리
- 연결 상태 실시간 모니터링
- 모니터링 간격 설정 가능
- 연결 테스트 및 검증
- 오류 로깅 및 알림

## 문제 해결

### 1. Ace Editor 로드 실패
- CDN 접근 불가 시 textarea로 자동 전환
- 콘솔 로그 확인

### 2. 드래그 앤 드롭 동작 안함
- 브라우저 호환성 확인
- JavaScript 오류 확인

### 3. 데이터베이스 연결 오류
- 데이터베이스 스키마 생성 확인
- JdbcTemplate 설정 확인
- 연결 정보 및 드라이버 파일 확인

### 4. 연결 모니터링 오류
- 모니터링 설정 확인 (MONITORING_ENABLED, MONITORING_INTERVAL)
- 연결 풀 상태 확인
- 로그 파일 확인

### 5. 권한 관련 오류
- 사용자 그룹 매핑 확인
- 그룹 연결 권한 확인
- 관리자 권한 확인

## 향후 개선 사항

1. 템플릿 파라미터 관리 기능
2. 템플릿 버전 관리
3. 템플릿 실행 히스토리
4. 템플릿 공유 기능
5. 템플릿 템플릿 기능 (템플릿에서 템플릿 생성)
6. 연결 상태 알림 기능 (이메일, SMS)
7. 연결 성능 통계 및 분석
8. 자동 백업 및 복구 기능
