package kr.Windmill.service;

import kr.Windmill.dto.connection.DatabaseConnectionDto;
import kr.Windmill.dto.connection.SftpConnectionDto;
import kr.Windmill.dto.connection.ConnectionStatusDto;
import kr.Windmill.mapper.ConnectionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis를 사용한 개선된 ConnectionService
 */
@Service
public class ConnectionServiceMyBatis {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionServiceMyBatis.class);
    
    @Autowired
    private ConnectionMapper connectionMapper;
    
    // 연결 상태 캐시
    private final Map<String, ConnectionStatusDto> connectionStatusMap = new ConcurrentHashMap<>();
    
    /**
     * 데이터베이스 연결 목록 조회
     */
    public List<DatabaseConnectionDto> getDatabaseConnections() {
        try {
            return connectionMapper.getDatabaseConnections();
        } catch (Exception e) {
            logger.error("데이터베이스 연결 목록 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * SFTP 연결 목록 조회
     */
    public List<SftpConnectionDto> getSftpConnections() {
        try {
            return connectionMapper.getSftpConnections();
        } catch (Exception e) {
            logger.error("SFTP 연결 목록 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 데이터베이스 연결 상세 조회
     */
    public DatabaseConnectionDto getDatabaseConnectionById(String connectionId) {
        try {
            return connectionMapper.getDatabaseConnectionById(connectionId);
        } catch (Exception e) {
            logger.error("데이터베이스 연결 상세 조회 실패: {} - {}", connectionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * SFTP 연결 상세 조회
     */
    public SftpConnectionDto getSftpConnectionById(String sftpConnectionId) {
        try {
            return connectionMapper.getSftpConnectionById(sftpConnectionId);
        } catch (Exception e) {
            logger.error("SFTP 연결 상세 조회 실패: {} - {}", sftpConnectionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 연결 테스트 SQL 조회
     */
    public String getConnectionTestSql(String connectionId) {
        try {
            return connectionMapper.getConnectionTestSql(connectionId);
        } catch (Exception e) {
            logger.error("연결 테스트 SQL 조회 실패: {} - {}", connectionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 권한이 있는 연결 목록 조회
     */
    public List<String> getAuthorizedConnections(String userId) {
        try {
            return connectionMapper.getAuthorizedConnections(userId);
        } catch (Exception e) {
            logger.error("권한이 있는 연결 목록 조회 실패: {} - {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 데이터베이스 연결 등록
     */
    @Transactional
    public boolean createDatabaseConnection(DatabaseConnectionDto connection) {
        try {
            // 연결 ID 생성
            if (connection.getConnectionId() == null || connection.getConnectionId().trim().isEmpty()) {
                connection.setConnectionId("DB_" + System.currentTimeMillis());
            }
            
            int result = connectionMapper.insertDatabaseConnection(connection);
            return result > 0;
        } catch (Exception e) {
            logger.error("데이터베이스 연결 등록 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 데이터베이스 연결 수정
     */
    @Transactional
    public boolean updateDatabaseConnection(DatabaseConnectionDto connection) {
        try {
            int result = connectionMapper.updateDatabaseConnection(connection);
            return result > 0;
        } catch (Exception e) {
            logger.error("데이터베이스 연결 수정 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * SFTP 연결 등록
     */
    @Transactional
    public boolean createSftpConnection(SftpConnectionDto connection) {
        try {
            // 연결 ID 생성
            if (connection.getSftpConnectionId() == null || connection.getSftpConnectionId().trim().isEmpty()) {
                connection.setSftpConnectionId("SFTP_" + System.currentTimeMillis());
            }
            
            int result = connectionMapper.insertSftpConnection(connection);
            return result > 0;
        } catch (Exception e) {
            logger.error("SFTP 연결 등록 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * SFTP 연결 수정
     */
    @Transactional
    public boolean updateSftpConnection(SftpConnectionDto connection) {
        try {
            int result = connectionMapper.updateSftpConnection(connection);
            return result > 0;
        } catch (Exception e) {
            logger.error("SFTP 연결 수정 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 데이터베이스 연결 삭제
     */
    @Transactional
    public boolean deleteDatabaseConnection(String connectionId) {
        try {
            int result = connectionMapper.deleteDatabaseConnection(connectionId);
            return result > 0;
        } catch (Exception e) {
            logger.error("데이터베이스 연결 삭제 실패: {} - {}", connectionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * SFTP 연결 삭제
     */
    @Transactional
    public boolean deleteSftpConnection(String sftpConnectionId) {
        try {
            int result = connectionMapper.deleteSftpConnection(sftpConnectionId);
            return result > 0;
        } catch (Exception e) {
            logger.error("SFTP 연결 삭제 실패: {} - {}", sftpConnectionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 데이터베이스 연결 존재 여부 확인
     */
    public boolean existsDatabaseConnection(String connectionId) {
        try {
            return connectionMapper.existsDatabaseConnection(connectionId);
        } catch (Exception e) {
            logger.error("데이터베이스 연결 존재 여부 확인 실패: {} - {}", connectionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * SFTP 연결 존재 여부 확인
     */
    public boolean existsSftpConnection(String sftpConnectionId) {
        try {
            return connectionMapper.existsSftpConnection(sftpConnectionId);
        } catch (Exception e) {
            logger.error("SFTP 연결 존재 여부 확인 실패: {} - {}", sftpConnectionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 활성 데이터베이스 연결 수 조회
     */
    public int getActiveDatabaseConnectionCount() {
        try {
            return connectionMapper.getActiveDatabaseConnectionCount();
        } catch (Exception e) {
            logger.error("활성 데이터베이스 연결 수 조회 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 활성 SFTP 연결 수 조회
     */
    public int getActiveSftpConnectionCount() {
        try {
            return connectionMapper.getActiveSftpConnectionCount();
        } catch (Exception e) {
            logger.error("활성 SFTP 연결 수 조회 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 연결 상태 업데이트
     */
    public void updateConnectionStatus(String connectionId, String status, String color, String errorMessage) {
        ConnectionStatusDto statusDto = new ConnectionStatusDto(connectionId, status, color);
        if (errorMessage != null) {
            statusDto.setErrorMessage(errorMessage);
        }
        connectionStatusMap.put(connectionId, statusDto);
    }
    
    /**
     * 연결 상태 조회
     */
    public ConnectionStatusDto getConnectionStatus(String connectionId) {
        return connectionStatusMap.get(connectionId);
    }
    
    /**
     * 모든 연결 상태 조회
     */
    public List<ConnectionStatusDto> getAllConnectionStatuses() {
        return new ArrayList<>(connectionStatusMap.values());
    }
}
