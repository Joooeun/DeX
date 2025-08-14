package kr.Windmill.mapper;

import kr.Windmill.dto.connection.DatabaseConnectionDto;
import kr.Windmill.dto.connection.SftpConnectionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 연결 매퍼 인터페이스
 */
@Mapper
public interface ConnectionMapper {
    
    /**
     * 데이터베이스 연결 목록 조회
     */
    List<DatabaseConnectionDto> getDatabaseConnections();
    
    /**
     * SFTP 연결 목록 조회
     */
    List<SftpConnectionDto> getSftpConnections();
    
    /**
     * 데이터베이스 연결 상세 조회
     */
    DatabaseConnectionDto getDatabaseConnectionById(@Param("connectionId") String connectionId);
    
    /**
     * SFTP 연결 상세 조회
     */
    SftpConnectionDto getSftpConnectionById(@Param("sftpConnectionId") String sftpConnectionId);
    
    /**
     * 연결 테스트 SQL 조회
     */
    String getConnectionTestSql(@Param("connectionId") String connectionId);
    
    /**
     * 권한이 있는 연결 목록 조회
     */
    List<String> getAuthorizedConnections(@Param("userId") String userId);
    
    /**
     * 데이터베이스 연결 등록
     */
    int insertDatabaseConnection(DatabaseConnectionDto connection);
    
    /**
     * 데이터베이스 연결 수정
     */
    int updateDatabaseConnection(DatabaseConnectionDto connection);
    
    /**
     * SFTP 연결 등록
     */
    int insertSftpConnection(SftpConnectionDto connection);
    
    /**
     * SFTP 연결 수정
     */
    int updateSftpConnection(SftpConnectionDto connection);
    
    /**
     * 데이터베이스 연결 삭제
     */
    int deleteDatabaseConnection(@Param("connectionId") String connectionId);
    
    /**
     * SFTP 연결 삭제
     */
    int deleteSftpConnection(@Param("sftpConnectionId") String sftpConnectionId);
    
    /**
     * 데이터베이스 연결 존재 여부 확인
     */
    boolean existsDatabaseConnection(@Param("connectionId") String connectionId);
    
    /**
     * SFTP 연결 존재 여부 확인
     */
    boolean existsSftpConnection(@Param("sftpConnectionId") String sftpConnectionId);
    
    /**
     * 활성 데이터베이스 연결 수 조회
     */
    int getActiveDatabaseConnectionCount();
    
    /**
     * 활성 SFTP 연결 수 조회
     */
    int getActiveSftpConnectionCount();
}
