# **Windmill 데이터베이스 스키마 설계 (누락된 핵심 테이블)**

## **📋 개요**
파일 기반 시스템에서 데이터베이스 기반 시스템으로 이전하기 위한 누락된 핵심 테이블들의 설계 문서입니다.

---

## **🔍 현재 파일 기반 구조 분석**

### **현재 파일 구조**
```
Root/
├── Connection/           # 연결 정보 (.properties 파일들)
│   ├── connection1.properties
│   ├── connection2.properties
│   └── ...
├── src/                 # SQL 템플릿 (.sql + .properties 파일들)
│   ├── 001_DashBoard/
│   │   ├── sql1.sql
│   │   ├── sql1.properties
│   │   ├── sql2.sql
│   │   └── sql2.properties
│   ├── 002_Reports/
│   └── ...
├── user/                # 사용자 정보 (파일명 = 사용자ID)
│   ├── admin
│   ├── user1
│   └── ...
└── jdbc/                # JDBC 드라이버 파일들
```

---

## **🏗️ 1단계: 누락된 핵심 테이블 설계**

### **1-1. SQL_TEMPLATE 테이블**

**목적**: SQL 템플릿 정보 관리
**파일 기반 → DB 변경사항:**
- 파일명 → TEMPLATE_ID
- 파일 경로 → CATEGORY_PATH
- .sql 파일 내용 → SQL_CONTENT

**주요 컬럼:**
- TEMPLATE_ID (PK) - 파일명 (예: sql1)
- TEMPLATE_NAME - 표시용 이름
- CATEGORY_PATH - 카테고리 경로 (예: 001_DashBoard, 002_Reports)
- SQL_CONTENT - SQL 파일 내용
- VERSION - 버전 관리
- STATUS - 활성/비활성 상태 (ACTIVE, INACTIVE, DRAFT)
- EXECUTION_LIMIT - 실행 결과 제한 (기존 LIMIT)
- REFRESH_TIMEOUT - 새로고침 간격 (기존 REFRESHTIMEOUT)
- CREATED_BY - 생성자
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_BY - 수정자
- MODIFIED_TIMESTAMP - 수정 시간

---

### **1-2. SQL_TEMPLATE_PARAMETER 테이블**

**목적**: SQL 템플릿의 파라미터 관리 (화면에서 입력받는 동적 파라미터)
**파일 기반 → DB 변경사항:**
- SQL 실행 시 사용되는 동적 파라미터들을 관리

**주요 컬럼:**
- PARAMETER_ID (PK) - 파라미터 ID
- TEMPLATE_ID (FK) - SQL 템플릿 참조
- PARAMETER_NAME - 파라미터 이름 (예: startDate, endDate, userId)
- PARAMETER_TYPE - 파라미터 타입 (STRING, NUMBER, DATE, BOOLEAN)
- PARAMETER_ORDER - 파라미터 순서
- IS_REQUIRED - 필수 여부
- DEFAULT_VALUE - 기본값
- DESCRIPTION - 파라미터 설명
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_TIMESTAMP - 수정 시간

---

### **1-3. SQL_TEMPLATE_SHORTCUT 테이블**

**목적**: SQL 템플릿의 단축키 관리
**파일 기반 → DB 변경사항:**
- .properties 파일의 SHORTKEY 설정을 별도 테이블로 관리

**주요 컬럼:**
- SHORTCUT_ID (PK) - 단축키 ID
- SOURCE_TEMPLATE_ID (FK) - 단축키가 있는 SQL 템플릿 (현재 템플릿)
- TARGET_TEMPLATE_ID (FK) - 단축키로 연결될 SQL 템플릿 (목적지 템플릿)
- SHORTCUT_KEY - 단축키 (예: F1, F2, Ctrl+S)
- SHORTCUT_NAME - 단축키 이름 (예: 조회, 상세보기)
- SHORTCUT_DESCRIPTION - 단축키 설명
- SOURCE_COLUMN_INDEXES - 소스 테이블에서 가져올 컬럼 인덱스 (예: 0,1,2)
- AUTO_EXECUTE - 자동 실행 여부 (true/false)
- IS_ACTIVE - 활성 여부
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_TIMESTAMP - 수정 시간

**설명:**
- **SOURCE_TEMPLATE_ID**: 단축키가 정의된 현재 SQL 템플릿
- **TARGET_TEMPLATE_ID**: 단축키를 눌렀을 때 이동할 SQL 템플릿
- **SOURCE_COLUMN_INDEXES**: 현재 결과 테이블에서 파라미터로 전달할 컬럼들의 인덱스
- **AUTO_EXECUTE**: 목적지 템플릿으로 이동 후 자동으로 SQL 실행할지 여부

---

### **1-3. DASHBOARD_TEMPLATE 테이블**

**목적**: 대시보드 전용 SQL 템플릿 관리 (추후 구현 예정)
**파일 기반 → DB 변경사항:**
- 대시보드 관련 설정을 별도 테이블로 분리

**주요 컬럼:**
- DASHBOARD_ID (PK) - 대시보드 ID
- TEMPLATE_ID (FK) - SQL 템플릿 참조
- DASHBOARD_NAME - 대시보드 이름
- REFRESH_INTERVAL - 새로고침 간격 (초)
- CHART_TYPE - 차트 타입 (LINE, BAR, PIE, TABLE)
- CHART_CONFIG - 차트 설정 (JSON)
- DISPLAY_ORDER - 표시 순서
- IS_ACTIVE - 활성 여부
- CREATED_BY - 생성자
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_BY - 수정자
- MODIFIED_TIMESTAMP - 수정 시간

**참고**: 현재 단계에서는 구현하지 않고 추후 단계에서 구현 예정

---

### **1-4. DATABASE_CONNECTION 테이블**

**목적**: 데이터베이스 연결 정보 관리
**파일 기반 → DB 변경사항:**
- .properties 파일 내용 → 개별 컬럼으로 분리

**주요 컬럼:**
- CONNECTION_ID (PK) - 연결 ID (파일명)
- CONNECTION_NAME - 표시용 연결명
- DB_TYPE - 데이터베이스 타입 (DB2, ORACLE, POSTGRESQL, TIBERO)
- HOST_IP - 호스트 IP
- PORT - 포트
- DATABASE_NAME - 데이터베이스명
- USERNAME - 사용자명
- PASSWORD - 비밀번호 (암호화 없음)
- JDBC_DRIVER_FILE - JDBC 드라이버 파일
- CONNECTION_POOL_SETTINGS - 연결 풀 설정 (JSON)
- CONNECTION_TIMEOUT - 연결 타임아웃
- QUERY_TIMEOUT - 쿼리 타임아웃
- MAX_POOL_SIZE - 최대 연결 풀 크기
- MIN_POOL_SIZE - 최소 연결 풀 크기
- STATUS - 연결 상태 (ACTIVE: 정상, INACTIVE: 비활성, ERROR: 오류, TESTING: 테스트중)
- LAST_CONNECTION_TEST - 마지막 연결 테스트 시간
- CONNECTION_TEST_RESULT - 연결 테스트 결과 (SUCCESS, FAIL, TIMEOUT)
- CREATED_BY - 생성자
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_BY - 수정자
- MODIFIED_TIMESTAMP - 수정 시간

---

### **1-5. SFTP_CONNECTION 테이블**

**목적**: SFTP 연결 정보 전용 관리 (FileController에서 사용)
**파일 기반 → DB 변경사항:**
- SFTP 연결 정보를 별도 테이블로 분리

**주요 컬럼:**
- SFTP_CONNECTION_ID (PK) - SFTP 연결 ID
- CONNECTION_NAME - 표시용 연결명
- HOST_IP - 호스트 IP
- PORT - 포트 (기본값: 22)
- USERNAME - 사용자명
- PASSWORD - 비밀번호 (암호화 없음)
- PRIVATE_KEY_PATH - 개인키 경로 (선택사항)
- REMOTE_PATH - 원격 기본 경로
- CONNECTION_TIMEOUT - 연결 타임아웃
- STATUS - 연결 상태 (ACTIVE, INACTIVE, ERROR, TESTING)
- LAST_CONNECTION_TEST - 마지막 연결 테스트 시간
- CONNECTION_TEST_RESULT - 연결 테스트 결과 (SUCCESS, FAIL, TIMEOUT)
- CREATED_BY - 생성자
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_BY - 수정자
- MODIFIED_TIMESTAMP - 수정 시간

---

### **1-6. SYSTEM_SETTING 테이블**

**목적**: 시스템 설정 관리 (system.properties 파일 대체)
**파일 기반 → DB 변경사항:**
- system.properties 파일 내용을 DB로 관리

**주요 컬럼:**
- SETTING_KEY (PK) - 설정 키
- SETTING_VALUE - 설정 값
- SETTING_TYPE - 설정 타입 (STRING, NUMBER, BOOLEAN, JSON)
- DESCRIPTION - 설정 설명
- CATEGORY - 설정 카테고리 (GENERAL, SECURITY, PERFORMANCE, UI, SYSTEM)
- IS_ENCRYPTED - 암호화 여부
- IS_SYSTEM - 시스템 설정 여부 (삭제 불가)
- IS_REQUIRED - 필수 설정 여부
- DEFAULT_VALUE - 기본값
- VALIDATION_RULE - 유효성 검사 규칙
- CREATED_TIMESTAMP - 생성 시간
- MODIFIED_TIMESTAMP - 수정 시간

---

## **📊 설계 검토 포인트**

### **✅ 개선된 점들**
1. **명확한 분리**: 파라미터, 단축키, SFTP 연결을 별도 테이블로 분리
2. **유연한 파라미터 관리**: 한 템플릿에 여러 파라미터 지원
3. **연결 타입별 최적화**: DB와 SFTP 연결을 각각에 맞게 설계
4. **명확한 상태 관리**: STATUS 필드에 구체적인 값 정의
5. **구조화된 데이터**: 파일 기반 → 정규화된 DB 구조
6. **메타데이터 추가**: 버전, 상태, 설명 등 관리 정보
7. **확장성**: JSON 필드로 유연한 설정 지원
8. **단축키 관리**: SQL 템플릿별 단축키 설정 지원

### **⚠️ 고려사항**
1. **테이블 수 증가**: 정규화로 인한 조인 복잡성
2. **마이그레이션 복잡성**: 파일 → 여러 테이블로 분산 이전
3. **기존 코드 수정**: FileController, SQLController 등 수정 필요
4. **성능 영향**: 대용량 CLOB 데이터 처리
5. **호환성**: 기존 API 인터페이스 유지 필요
6. **롤백 계획**: 문제 발생 시 파일 기반으로 복구

---

## **🔗 테이블 관계도**

```
USERS (기존)
├── USER_GROUP (기존)
├── USER_GROUP_MAPPING (기존)
├── SQL_TEMPLATE_PERMISSION (기존)
├── CONNECTION_PERMISSION (기존)
├── AUDIT_LOG (기존)
└── USER_SESSION (기존)

SQL_TEMPLATE (신규)
├── SQL_TEMPLATE_PARAMETER (신규)
└── SQL_TEMPLATE_SHORTCUT (신규)

DATABASE_CONNECTION (신규)
SFTP_CONNECTION (신규)
SYSTEM_SETTING (신규)

DASHBOARD_TEMPLATE (신규 - 추후 구현)
```

---

## **📝 다음 단계**

1. **스키마 생성 스크립트 작성**
2. **마이그레이션 유틸리티 개발**
3. **서비스 계층 수정**
4. **테스트 및 검증**

---

**작성일**: 2025년 8월 11일  
**작성자**: Windmill 개발팀  
**버전**: 1.0
