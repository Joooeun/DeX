# DTO 구조 가이드

## 개요
DTO(Data Transfer Object) 패키지를 도메인별로 분리하여 체계적으로 관리합니다.

## 패키지 구조

```
kr.Windmill.dto/
├── common/                    # 공통 DTO
│   ├── BaseDto.java          # 기본 공통 필드
│   └── ApiResponseDto.java   # API 응답 공통 DTO
├── user/                     # 사용자 관련 DTO
│   ├── UserDto.java          # 사용자 DTO
│   └── UserGroupDto.java     # 사용자 그룹 DTO
├── connection/               # 연결 관련 DTO
│   ├── DatabaseConnectionDto.java  # 데이터베이스 연결 DTO
│   ├── SftpConnectionDto.java      # SFTP 연결 DTO
│   ├── ConnectionDto.java          # 연결 설정 DTO (기존 ConnectionDTO)
│   └── ConnectionStatusDto.java    # 연결 상태 DTO (기존 ConnectionStatusDTO)
├── permission/               # 권한 관련 DTO
│   └── PermissionDto.java    # 권한 DTO
├── sqltemplate/              # SQL 템플릿 관련 DTO
│   ├── SqlTemplateDto.java           # SQL 템플릿 DTO
│   └── SqlTemplateParameterDto.java  # SQL 템플릿 파라미터 DTO
├── system/                   # 시스템 관련 DTO
│   └── DexStatusDto.java     # DeX 시스템 상태 DTO (기존 DexStatusDTO)
└── log/                      # 로그 관련 DTO
    └── LogInfoDto.java       # 로그 정보 DTO (기존 LogInfoDTO)
```

## 기존 DTO 이동 내역

| 기존 파일 | 새 위치 | 변경 사항 |
|-----------|---------|-----------|
| `service/ConnectionDTO.java` | `dto/connection/ConnectionDto.java` | 패키지 이동, 네이밍 개선 |
| `service/ConnectionStatusDTO.java` | `dto/connection/ConnectionStatusDto.java` | 패키지 이동, 네이밍 개선 |
| `service/DexStatusDTO.java` | `dto/system/DexStatusDto.java` | 패키지 이동, 네이밍 개선 |
| `service/LogInfoDTO.java` | `dto/log/LogInfoDto.java` | 패키지 이동, 네이밍 개선 |

## 사용 방법

### 1. 공통 DTO 사용
```java
// API 응답
ApiResponseDto<UserDto> response = ApiResponseDto.success("사용자 조회 성공", userDto);

// 기본 DTO 상속
public class CustomDto extends BaseDto {
    // 추가 필드들...
}
```

### 2. 도메인별 DTO 사용
```java
// 사용자 관련
UserDto user = new UserDto("user001", "홍길동");
UserGroupDto group = new UserGroupDto("group001", "관리자");

// 연결 관련
DatabaseConnectionDto dbConn = new DatabaseConnectionDto("DB_001", "DB2", "192.168.1.100");
SftpConnectionDto sftpConn = new SftpConnectionDto("SFTP_001", "192.168.1.101", "sftpuser");
ConnectionDto conn = new ConnectionDto("DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://localhost:50000/SAMPLE");
ConnectionStatusDto status = new ConnectionStatusDto("DB_001", "connected", "#28a745");

// 권한 관련
PermissionDto permission = new PermissionDto("group001", "DB_001", "DB");

// SQL 템플릿 관련
SqlTemplateDto template = new SqlTemplateDto("TEMP_001", "사용자 조회", "SELECT * FROM USERS");

// 시스템 관련
DexStatusDto dexStatus = new DexStatusDto("database", "데이터베이스", "running", "#28a745", "정상 동작 중");

// 로그 관련
LogInfoDto logInfo = new LogInfoDto();
logInfo.setConnectionId("DB_001");
logInfo.setSql("SELECT * FROM USERS");
```

## 장점

1. **도메인 분리**: 관련된 DTO들을 패키지별로 분리하여 관리
2. **재사용성**: 공통 DTO를 통해 중복 코드 제거
3. **확장성**: 새로운 도메인 추가 시 패키지만 추가하면 됨
4. **가독성**: 패키지명만으로도 DTO의 용도를 쉽게 파악 가능
5. **유지보수성**: 도메인별로 분리되어 있어 수정 시 영향 범위 최소화
6. **일관성**: 네이밍 규칙 통일로 코드 일관성 향상

## 네이밍 규칙

- **클래스명**: `{도메인명}Dto.java` (기존 DTO → Dto로 변경)
- **패키지명**: 도메인명을 소문자로
- **필드명**: camelCase 사용
- **상수**: UPPER_SNAKE_CASE 사용

## 추가 예정 DTO

- `file/` - 파일 관련 DTO
- `dashboard/` - 대시보드 관련 DTO
- `monitoring/` - 모니터링 관련 DTO
- `audit/` - 감사 관련 DTO

## 마이그레이션 가이드

기존 코드에서 DTO 사용 시 import 문을 다음과 같이 변경해야 합니다:

```java
// 기존
import kr.Windmill.service.ConnectionDTO;
import kr.Windmill.service.DexStatusDTO;

// 변경 후
import kr.Windmill.dto.connection.ConnectionDto;
import kr.Windmill.dto.system.DexStatusDto;
```
