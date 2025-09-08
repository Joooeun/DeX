-- =====================================================
-- Windmill SQL 템플릿 샘플 데이터
-- =====================================================
-- DB2와 PostgreSQL 샘플 데이터를 활용한 다양한 SQL 템플릿
-- 실행 전 COMPLETE_DATABASE_SCHEMA.sql이 먼저 실행되어야 함

-- =====================================================
-- 0. 기존 데이터 삭제 (초기화)
-- =====================================================

-- 외래키 제약조건으로 인한 삭제 순서 주의
-- 단축키 삭제
DELETE FROM SQL_TEMPLATE_SHORTCUT;

-- 파라미터 삭제
DELETE FROM SQL_TEMPLATE_PARAMETER;

-- 카테고리 매핑 삭제
DELETE FROM SQL_TEMPLATE_CATEGORY_MAPPING;

-- SQL 내용 삭제
DELETE FROM SQL_CONTENT;

-- SQL 템플릿 삭제
DELETE FROM SQL_TEMPLATE;

-- 카테고리는 기본 카테고리만 유지하고 추가 카테고리만 삭제
DELETE FROM SQL_TEMPLATE_CATEGORY WHERE CATEGORY_ID NOT IN ('DASHBOARD', 'REPORT', 'MONITORING', 'ADMIN', 'UTILITY');

-- 시퀀스 리셋 (DB2의 경우)
-- ALTER SEQUENCE SQL_TEMPLATE_PARAMETER_SEQ RESTART WITH 1;
-- ALTER SEQUENCE SQL_TEMPLATE_SHORTCUT_SEQ RESTART WITH 1;

-- =====================================================
-- 1. DB 모니터링 템플릿 (필수)
-- =====================================================

-- DB2 연결 상태 모니터링 (DB2 12 호환)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_CONNECTION_STATUS',
    'DB2 연결 상태 모니터링',
    'DB2 12 데이터베이스 연결 상태 및 활성 세션 수 조회',
    'SELECT 
        APPLICATION_HANDLE,
        APPLICATION_NAME,
        CLIENT_USERID,
        CLIENT_WRKSTNNAME,
        UOW_START_TIME,
        STMT_START_TIME,
        TOTAL_CPU_TIME
    FROM SYSIBMADM.APPLICATIONS 
    WHERE APPLICATION_HANDLE > 0
    ORDER BY UOW_START_TIME DESC',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- PostgreSQL 연결 상태 모니터링
INSERT INTO SQL_CONTENT (
    TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, 
    VERSION, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_CONNECTION_STATUS',
    'pg',
    'SELECT 
        pid,
        usename,
        application_name,
        client_addr,
        client_hostname,
        client_port,
        backend_start,
        state,
        query_start,
        state_change,
        wait_event_type,
        wait_event,
        query
    FROM pg_stat_activity 
    WHERE pid <> pg_backend_pid()
    ORDER BY backend_start DESC',
    1,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- DB2 락 대기 모니터링 (DB2 12 호환)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, CHART_MAPPING, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_LOCK_WAIT_MONITOR',
    'DB2 락 대기 모니터링',
    'DB2 12 데이터베이스에서 발생하는 락 대기 상황 모니터링',
    'SELECT 25 FROM SYSIBM.SYSDUMMY1',
    'test_db',
    'LOCK_WAIT_COUNT',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- PostgreSQL 락 대기 모니터링
INSERT INTO SQL_CONTENT (
    TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, 
    VERSION, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_LOCK_WAIT_MONITOR',
    'pg',
    'SELECT 
        l.locktype,
        l.database,
        l.relation::regclass,
        l.page,
        l.tuple,
        l.virtualxid,
        l.transactionid,
        l.classid,
        l.objid,
        l.objsubid,
        l.virtualtransaction,
        l.pid,
        l.mode,
        l.granted,
        a.usename,
        a.query,
        a.query_start,
        a.state
    FROM pg_locks l
    LEFT JOIN pg_stat_activity a ON l.pid = a.pid
    WHERE NOT l.granted
    ORDER BY a.query_start',
    1,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- =====================================================
-- 2. DB2 샘플 데이터 활용 템플릿
-- =====================================================

-- 직원 정보 조회 (파라미터: 부서코드)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'EMPLOYEE_DEPT_QUERY',
    '부서별 직원 정보 조회',
    '특정 부서의 직원 정보를 조회합니다 (파라미터 없으면 전체 조회)',
    'SELECT 
        EMPNO,
        FIRSTNME,
        MIDINIT,
        LASTNAME,
        WORKDEPT,
        PHONENO,
        HIREDATE,
        JOB,
        EDLEVEL,
        SEX,
        BIRTHDATE,
        SALARY,
        BONUS,
        COMM
    FROM EMPLOYEE 
    WHERE (${deptCode}= '''' OR WORKDEPT = ${deptCode})
    ORDER BY WORKDEPT, SALARY DESC',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 직원 정보 조회 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES (
    'EMPLOYEE_DEPT_QUERY', 'deptCode', 'STRING', 1, 
    FALSE, '', '부서 코드 (빈 값이면 전체 조회, 예: A00, B01, C01)'
);

-- 급여 통계 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'SALARY_STATISTICS',
    '급여 통계 조회',
    '부서별 급여 통계를 조회합니다',
    'SELECT 
        WORKDEPT,
        COUNT(*) AS EMP_COUNT,
        MIN(SALARY) AS MIN_SALARY,
        MAX(SALARY) AS MAX_SALARY,
        AVG(SALARY) AS AVG_SALARY,
        SUM(SALARY) AS TOTAL_SALARY
    FROM EMPLOYEE 
    WHERE SALARY > 0
    GROUP BY WORKDEPT
    ORDER BY AVG_SALARY DESC',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 고객 정보 XML 파싱
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'CUSTOMER_XML_PARSE',
    '고객 XML 정보 파싱',
    'CUSTOMER 테이블의 XML 데이터를 파싱하여 고객 정보를 조회합니다 (파라미터 없으면 전체 조회)',
    'SELECT 
        CID,
        XMLSERIALIZE(INFO AS VARCHAR(1000)) AS CUSTOMER_INFO
    FROM CUSTOMER 
    WHERE (${customerId}= '''' OR CID = CAST(${customerId} AS INTEGER))
    ORDER BY CID',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 고객 정보 XML 파싱 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES (
    'CUSTOMER_XML_PARSE', 'customerId', 'NUMBER', 1, 
    FALSE, NULL, '고객 ID (NULL이면 전체 조회)'
);

-- 제품 정보 조회 (가격 범위)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'PRODUCT_PRICE_RANGE',
    '가격대별 제품 조회',
    '특정 가격 범위의 제품을 조회합니다 (파라미터 없으면 전체 조회)',
    'SELECT 
        PID,
        NAME,
        PRICE,
        PROMOPRICE,
        PROMOSTART,
        PROMOEND,
        CASE 
            WHEN PROMOPRICE IS NOT NULL AND CURRENT DATE BETWEEN PROMOSTART AND PROMOEND 
            THEN PROMOPRICE 
            ELSE PRICE 
        END AS CURRENT_PRICE
    FROM PRODUCT 
    WHERE (${minPrice}= '''' OR ${maxPrice}= '''' OR PRICE BETWEEN CAST(${minPrice} AS DECIMAL(10,2)) AND CAST(${maxPrice} AS DECIMAL(10,2)))
    ORDER BY CURRENT_PRICE',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 제품 정보 조회 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES 
('PRODUCT_PRICE_RANGE', 'minPrice', 'NUMBER', 1, FALSE, NULL, '최소 가격 (NULL이면 전체 조회)'),
('PRODUCT_PRICE_RANGE', 'maxPrice', 'NUMBER', 2, FALSE, NULL, '최대 가격 (NULL이면 전체 조회)');

-- =====================================================
-- 3. PostgreSQL 샘플 데이터 활용 템플릿
-- =====================================================

-- 고객 주소 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'CUSTOMER_ADDRESS_QUERY',
    '고객 주소 정보 조회',
    '특정 도시의 고객 주소 정보를 조회합니다',
    'SELECT 
        CID,
        XMLSERIALIZE(INFO AS VARCHAR(1000)) AS CUSTOMER_INFO
    FROM CUSTOMER 
    WHERE (${cityName}= '''' OR 1=1)
    ORDER BY CID',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 고객 주소 정보 조회 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES (
    'CUSTOMER_ADDRESS_QUERY', 'cityName', 'STRING', 1, 
    FALSE, 'Seoul', '도시명'
);

-- 장바구니 통계 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'CART_STATISTICS',
    '장바구니 통계 조회',
    '장바구니별 상품 수량 통계를 조회합니다',
    'SELECT 
        CID,
        XMLSERIALIZE(INFO AS VARCHAR(1000)) AS CUSTOMER_INFO
    FROM CUSTOMER 
    WHERE (${startDate}= '''' OR 1=1)
    ORDER BY CID',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 장바구니 통계 조회 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES (
    'CART_STATISTICS', 'startDate', 'DATE', 1, 
    FALSE, '2024-01-01', '조회 시작 날짜'
);

-- =====================================================
-- 4. 고급 SQL 기능 템플릿
-- =====================================================

-- 윈도우 함수를 활용한 순위 조회 (DB2)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'EMPLOYEE_SALARY_RANK',
    '급여 순위 조회 (윈도우 함수)',
    '부서별 급여 순위를 윈도우 함수로 조회합니다',
    'SELECT 
        EMPNO,
        FIRSTNME,
        LASTNAME,
        WORKDEPT,
        SALARY,
        ROW_NUMBER() OVER (PARTITION BY WORKDEPT ORDER BY SALARY DESC) AS DEPT_RANK,
        RANK() OVER (ORDER BY SALARY DESC) AS OVERALL_RANK,
        DENSE_RANK() OVER (ORDER BY SALARY DESC) AS DENSE_RANK
    FROM EMPLOYEE 
    WHERE SALARY > 0
    ORDER BY WORKDEPT, SALARY DESC',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 윈도우 함수를 활용한 순위 조회 (PostgreSQL)
INSERT INTO SQL_CONTENT (
    TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, 
    VERSION, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'EMPLOYEE_SALARY_RANK',
    'pg',
    'SELECT 
        customer_id,
        address_id,
        city,
        postal_code
    FROM addresses 
    WHERE city = ${cityName}
    ORDER BY city, postal_code',
    1,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 재귀 CTE를 활용한 계층 구조 조회 (DB2)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'ORG_HIERARCHY',
    '조직 계층 구조 조회',
    'ORG 테이블의 계층 구조를 재귀 CTE로 조회합니다',
    'SELECT 
        deptnumb,
        deptname,
        manager
    FROM ORG 
    ORDER BY deptnumb',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 재귀 CTE를 활용한 계층 구조 조회 (PostgreSQL)
INSERT INTO SQL_CONTENT (
    TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, 
    VERSION, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'ORG_HIERARCHY',
    'pg',
    'SELECT 
        address_id,
        customer_id,
        city
    FROM addresses 
    ORDER BY customer_id, address_id',
    1,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- =====================================================
-- 5. 데이터 조작 템플릿 (INSERT, UPDATE, DELETE)
-- =====================================================

-- 직원 정보 삽입
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'INSERT_EMPLOYEE',
    '직원 정보 등록',
    '새로운 직원 정보를 등록합니다',
    'INSERT INTO EMPLOYEE (
        EMPNO, FIRSTNME, MIDINIT, LASTNAME, WORKDEPT, 
        PHONENO, HIREDATE, JOB, EDLEVEL, SEX, BIRTHDATE, SALARY
    ) VALUES (
        ${empno},
        ${firstname},
        ${midinit},
        ${lastname},
        ${workdept},
        ${phone},
        DATE(${hiredate}),
        ${job},
        ${edlevel},
        ${sex},
        DATE(${birthdate}),
        ${salary}
    )',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 직원 정보 삽입 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DESCRIPTION
) VALUES 
('INSERT_EMPLOYEE', 'empno', 'STRING', 1, TRUE, '직원 번호'),
('INSERT_EMPLOYEE', 'firstname', 'STRING', 2, TRUE, '이름'),
('INSERT_EMPLOYEE', 'midinit', 'STRING', 3, FALSE, '중간 이니셜'),
('INSERT_EMPLOYEE', 'lastname', 'STRING', 4, TRUE, '성'),
('INSERT_EMPLOYEE', 'workdept', 'STRING', 5, TRUE, '부서 코드'),
('INSERT_EMPLOYEE', 'phone', 'STRING', 6, FALSE, '전화번호'),
('INSERT_EMPLOYEE', 'hiredate', 'DATE', 7, TRUE, '입사일'),
('INSERT_EMPLOYEE', 'job', 'STRING', 8, TRUE, '직책'),
('INSERT_EMPLOYEE', 'edlevel', 'NUMBER', 9, TRUE, '교육 수준'),
('INSERT_EMPLOYEE', 'sex', 'STRING', 10, TRUE, '성별 (M/F)'),
('INSERT_EMPLOYEE', 'birthdate', 'DATE', 11, TRUE, '생년월일'),
('INSERT_EMPLOYEE', 'salary', 'NUMBER', 12, TRUE, '급여');

-- 급여 업데이트
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'UPDATE_EMPLOYEE_SALARY',
    '직원 급여 수정',
    '특정 직원의 급여를 수정합니다',
    'UPDATE EMPLOYEE 
    SET SALARY = ${newSalary}
    WHERE EMPNO = ${empno}',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 급여 업데이트 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DESCRIPTION
) VALUES 
('UPDATE_EMPLOYEE_SALARY', 'empno', 'STRING', 1, TRUE, '직원 번호'),
('UPDATE_EMPLOYEE_SALARY', 'newSalary', 'NUMBER', 2, TRUE, '새 급여');

-- 고객 주소 삭제 (PostgreSQL)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DELETE_CUSTOMER_ADDRESS',
    '고객 주소 삭제',
    '특정 고객의 주소를 삭제합니다',
    'DELETE FROM addresses 
    WHERE address_id = ${addressId}',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 고객 주소 삭제 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DESCRIPTION
) VALUES (
    'DELETE_CUSTOMER_ADDRESS', 'addressId', 'NUMBER', 1, TRUE, '주소 ID'
);

-- =====================================================
-- 6. 집계 및 분석 템플릿
-- =====================================================

-- 월별 입사자 통계
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'MONTHLY_HIRE_STATISTICS',
    '월별 입사자 통계',
    '월별 입사자 수와 급여 통계를 조회합니다',
    'SELECT 
        YEAR(HIREDATE) AS hire_year,
        MONTH(HIREDATE) AS hire_month,
        COUNT(*) AS hire_count,
        AVG(SALARY) AS avg_salary,
        MIN(SALARY) AS min_salary,
        MAX(SALARY) AS max_salary
    FROM EMPLOYEE 
    WHERE (${startYear}= '''' OR HIREDATE >= DATE(${startYear}-01-01))
    GROUP BY YEAR(HIREDATE), MONTH(HIREDATE)
    ORDER BY hire_year, hire_month',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 월별 입사자 통계 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES (
    'MONTHLY_HIRE_STATISTICS', 'startYear', 'NUMBER', 1, 
    FALSE, '2000', '조회 시작 연도'
);

-- 부서별 성별 급여 분석
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DEPT_GENDER_SALARY_ANALYSIS',
    '부서별 성별 급여 분석',
    '부서별 성별 급여 차이를 분석합니다',
    'SELECT 
        WORKDEPT,
        SEX,
        COUNT(*) AS emp_count,
        AVG(SALARY) AS avg_salary,
        MIN(SALARY) AS min_salary,
        MAX(SALARY) AS max_salary
    FROM EMPLOYEE 
    WHERE SALARY > 0
    GROUP BY WORKDEPT, SEX
    ORDER BY WORKDEPT, SEX',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- =====================================================
-- 7. 고급 분석 템플릿
-- =====================================================

-- 이동 평균 계산 (DB2)
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'SALARY_MOVING_AVERAGE',
    '급여 이동 평균 계산',
    '급여 데이터의 이동 평균을 계산합니다',
    'SELECT 
        EMPNO,
        FIRSTNME,
        LASTNAME,
        SALARY,
        AVG(SALARY) OVER (
            ORDER BY SALARY 
            ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING
        ) AS moving_avg_5
    FROM EMPLOYEE 
    WHERE SALARY > 0
    ORDER BY SALARY',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 이동 평균 계산 (PostgreSQL)
INSERT INTO SQL_CONTENT (
    TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, 
    VERSION, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'SALARY_MOVING_AVERAGE',
    'pg',
    'SELECT 
        customer_id,
        address_id,
        postal_code,
        AVG(postal_code) OVER (
            ORDER BY postal_code 
            ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING
        ) AS moving_avg_5
    FROM addresses 
    ORDER BY postal_code',
    1,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- =====================================================
-- 8. DBA용 데이터베이스 관리 템플릿
-- =====================================================

-- DB2 테이블스페이스 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_TABLESPACE_INFO',
    'DB2 테이블스페이스 정보',
    'DB2 테이블스페이스 사용량 및 상태 정보를 조회합니다',
    'SELECT 
        TBSP_NAME,
        TBSP_TYPE,
        TBSP_STATE,
        TBSP_TOTAL_PAGES,
        TBSP_USED_PAGES,
        TBSP_FREE_PAGES
    FROM SYSCAT.TABLESPACES
    ORDER BY TBSP_NAME',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- DB2 테이블 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_TABLE_INFO',
    'DB2 테이블 정보 조회',
    'DB2 테이블의 상세 정보를 조회합니다',
    'SELECT 
        TABSCHEMA,
        TABNAME,
        STATUS,
        CARD,
        NPAGES,
        FPAGES,
        COLCOUNT,
        TBSPACE
    FROM SYSCAT.TABLES
    WHERE TABSCHEMA NOT LIKE ''SYS%''
    ORDER BY TABNAME',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- DB2 인덱스 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_INDEX_INFO',
    'DB2 인덱스 정보 조회',
    'DB2 인덱스의 상세 정보를 조회합니다',
    'SELECT 
        INDSCHEMA,
        INDNAME,
        TABSCHEMA,
        TABNAME,
        UNIQUERULE,
        COLNAMES,
        COLCOUNT
    FROM SYSCAT.INDEXES
    WHERE TABSCHEMA NOT LIKE ''SYS%''
    ORDER BY INDNAME',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- DB2 컬럼 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_COLUMN_INFO',
    'DB2 컬럼 정보 조회',
    '특정 테이블의 컬럼 정보를 조회합니다',
    'SELECT 
        TABSCHEMA,
        TABNAME,
        COLNAME,
        COLNO,
        TYPENAME,
        LENGTH,
        SCALE,
        NULLS,
        DEFAULT
    FROM SYSCAT.COLUMNS
    WHERE TABSCHEMA = ${schemaName}
    AND TABNAME = ${tableName}
    ORDER BY COLNO',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- DB2 컬럼 정보 조회 파라미터
INSERT INTO SQL_TEMPLATE_PARAMETER (
    TEMPLATE_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_ORDER, 
    IS_REQUIRED, DEFAULT_VALUE, DESCRIPTION
) VALUES 
('DB2_COLUMN_INFO', 'schemaName', 'STRING', 1, FALSE, 'DB2INST1', '스키마명'),
('DB2_COLUMN_INFO', 'tableName', 'STRING', 2, FALSE, 'EMPLOYEE', '테이블명');

-- DB2 실행 중인 쿼리 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_ACTIVE_QUERIES',
    'DB2 실행 중인 쿼리 조회',
    '현재 실행 중인 쿼리와 성능 정보를 조회합니다',
    'SELECT 
        APPLICATION_HANDLE,
        APPLICATION_NAME,
        CLIENT_USERID,
        CLIENT_WRKSTNNAME,
        UOW_START_TIME,
        STMT_START_TIME,
        STMT_TEXT,
        TOTAL_CPU_TIME
    FROM SYSIBMADM.APPLICATIONS
    WHERE APPLICATION_HANDLE > 0
    AND STMT_TEXT IS NOT NULL
    ORDER BY STMT_START_TIME DESC',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- DB2 버퍼풀 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_BUFFERPOOL_INFO',
    'DB2 버퍼풀 정보 조회',
    'DB2 버퍼풀 사용량 및 성능 정보를 조회합니다',
    'SELECT 
        BP_NAME,
        NPAGES,
        PAGESIZE
    FROM SYSCAT.BUFFERPOOLS
    ORDER BY BP_NAME',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- PostgreSQL 테이블 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'PG_TABLE_INFO',
    'PostgreSQL 테이블 정보 조회',
    'PostgreSQL 테이블의 상세 정보를 조회합니다',
    'SELECT 
        schemaname,
        tablename,
        tableowner,
        tablespace,
        hasindexes,
        hasrules,
        hastriggers
    FROM pg_tables
    WHERE schemaname NOT IN (''information_schema'', ''pg_catalog'')
    ORDER BY tablename',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- PostgreSQL 인덱스 정보 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'PG_INDEX_INFO',
    'PostgreSQL 인덱스 정보 조회',
    'PostgreSQL 인덱스의 상세 정보를 조회합니다',
    'SELECT 
        schemaname,
        tablename,
        indexname,
        indexdef
    FROM pg_indexes
    WHERE schemaname NOT IN (''information_schema'', ''pg_catalog'')
    ORDER BY indexname',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- PostgreSQL 데이터베이스 크기 조회
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, VERSION, STATUS, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'PG_DATABASE_SIZE',
    'PostgreSQL 데이터베이스 크기 조회',
    'PostgreSQL 데이터베이스별 크기 정보를 조회합니다',
    'SELECT 
        datname as database_name,
        pg_size_pretty(pg_database_size(datname)) as size,
        pg_database_size(datname) as size_bytes
    FROM pg_database
    WHERE datname NOT IN (''template0'', ''template1'')
    ORDER BY pg_database_size(datname) DESC',
    'test_db',
    1,
    'ACTIVE',
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- =====================================================
-- 9. 카테고리 매핑
-- =====================================================

-- 모니터링 카테고리에 DB 모니터링 템플릿들 매핑
INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES
('DB2_CONNECTION_STATUS', 'MONITORING', 1, 'SYSTEM'),
('DB2_LOCK_WAIT_MONITOR', 'MONITORING', 2, 'SYSTEM');

-- 리포트 카테고리에 통계 템플릿들 매핑
INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES
('EMPLOYEE_DEPT_QUERY', 'REPORT', 1, 'SYSTEM'),
('SALARY_STATISTICS', 'REPORT', 2, 'SYSTEM'),
('CUSTOMER_XML_PARSE', 'REPORT', 3, 'SYSTEM'),
('PRODUCT_PRICE_RANGE', 'REPORT', 4, 'SYSTEM'),
('CUSTOMER_ADDRESS_QUERY', 'REPORT', 5, 'SYSTEM'),
('CART_STATISTICS', 'REPORT', 6, 'SYSTEM'),
('MONTHLY_HIRE_STATISTICS', 'REPORT', 7, 'SYSTEM'),
('DEPT_GENDER_SALARY_ANALYSIS', 'REPORT', 8, 'SYSTEM');

-- 유틸리티 카테고리에 고급 기능 템플릿들 매핑
INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES
('EMPLOYEE_SALARY_RANK', 'UTILITY', 1, 'SYSTEM'),
('ORG_HIERARCHY', 'UTILITY', 2, 'SYSTEM'),
('SALARY_MOVING_AVERAGE', 'UTILITY', 3, 'SYSTEM');

-- 관리 카테고리에 데이터 조작 템플릿들 매핑
INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES
('INSERT_EMPLOYEE', 'ADMIN', 1, 'SYSTEM'),
('UPDATE_EMPLOYEE_SALARY', 'ADMIN', 2, 'SYSTEM'),
('DELETE_CUSTOMER_ADDRESS', 'ADMIN', 3, 'SYSTEM');

-- DBA용 템플릿들을 관리 카테고리에 매핑
INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES
('DB2_TABLESPACE_INFO', 'ADMIN', 4, 'SYSTEM'),
('DB2_TABLE_INFO', 'ADMIN', 5, 'SYSTEM'),
('DB2_INDEX_INFO', 'ADMIN', 6, 'SYSTEM'),
('DB2_COLUMN_INFO', 'ADMIN', 7, 'SYSTEM'),
('DB2_ACTIVE_QUERIES', 'ADMIN', 8, 'SYSTEM'),
('DB2_BUFFERPOOL_INFO', 'ADMIN', 9, 'SYSTEM'),
('PG_TABLE_INFO', 'ADMIN', 10, 'SYSTEM'),
('PG_INDEX_INFO', 'ADMIN', 11, 'SYSTEM'),
('PG_DATABASE_SIZE', 'ADMIN', 12, 'SYSTEM');

-- =====================================================
-- 9. 단축키 설정
-- =====================================================

-- 직원 정보 조회에서 급여 통계로 이동
INSERT INTO SQL_TEMPLATE_SHORTCUT (
    SOURCE_TEMPLATE_ID, TARGET_TEMPLATE_ID, SHORTCUT_KEY, 
    SHORTCUT_NAME, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, AUTO_EXECUTE
) VALUES (
    'EMPLOYEE_DEPT_QUERY', 'SALARY_STATISTICS', 'F1', 
    '급여 통계 보기', '선택한 부서의 급여 통계를 조회합니다', '3', TRUE
);

-- 급여 통계에서 급여 순위로 이동
INSERT INTO SQL_TEMPLATE_SHORTCUT (
    SOURCE_TEMPLATE_ID, TARGET_TEMPLATE_ID, SHORTCUT_KEY, 
    SHORTCUT_NAME, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, AUTO_EXECUTE
) VALUES (
    'SALARY_STATISTICS', 'EMPLOYEE_SALARY_RANK', 'F2', 
    '급여 순위 보기', '급여 순위를 조회합니다', '0', TRUE
);

-- 고객 정보에서 주소 정보로 이동
INSERT INTO SQL_TEMPLATE_SHORTCUT (
    SOURCE_TEMPLATE_ID, TARGET_TEMPLATE_ID, SHORTCUT_KEY, 
    SHORTCUT_NAME, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, AUTO_EXECUTE
) VALUES (
    'CUSTOMER_XML_PARSE', 'CUSTOMER_ADDRESS_QUERY', 'F1', 
    '주소 정보 보기', '고객의 주소 정보를 조회합니다', '0', TRUE
);

-- =====================================================
-- 10. 차트 매핑용 템플릿 (대시보드용)
-- =====================================================

-- DB2 연결 수 차트
INSERT INTO SQL_TEMPLATE (
    TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, 
    ACCESSIBLE_CONNECTION_IDS, CHART_MAPPING, VERSION, STATUS, 
    EXECUTION_LIMIT, REFRESH_TIMEOUT, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_CONNECTION_CHART',
    'DB2 연결 수 차트',
    'DB2 활성 연결 수를 차트로 표시합니다',
    'SELECT 
        COUNT(*) as connection_count
    FROM SYSIBMADM.APPLICATIONS 
    WHERE APPLICATION_HANDLE > 0',
    'test_db',
    'CONNECTION_COUNT',
    1,
    'ACTIVE',
    1000,
    10,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- PostgreSQL 연결 수 차트
INSERT INTO SQL_CONTENT (
    TEMPLATE_ID, CONNECTION_ID, SQL_CONTENT, 
    VERSION, CREATED_BY, CREATED_TIMESTAMP
) VALUES (
    'DB2_CONNECTION_CHART',
    'pg',
    'SELECT 
        COUNT(*) as connection_count
    FROM pg_stat_activity 
    WHERE pid <> pg_backend_pid()',
    1,
    'SYSTEM',
    CURRENT TIMESTAMP
);

-- 차트 템플릿을 대시보드 카테고리에 매핑
INSERT INTO SQL_TEMPLATE_CATEGORY_MAPPING (TEMPLATE_ID, CATEGORY_ID, MAPPING_ORDER, CREATED_BY) VALUES
('DB2_CONNECTION_CHART', 'DASHBOARD', 1, 'SYSTEM');

-- =====================================================
-- SQL 템플릿 샘플 데이터 생성 완료
-- =====================================================
