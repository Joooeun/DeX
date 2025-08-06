package kr.Windmill.util;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC 드라이버 정리를 위한 유틸리티 클래스
 */
public class JdbcDriverCleanup {
    
    private static final Logger logger = LoggerFactory.getLogger(JdbcDriverCleanup.class);
    
    /**
     * JDBC 드라이버 정리 작업을 수행합니다.
     */
    public static void cleanupDrivers() {
        logger.info("JDBC 드라이버 정리 시작...");
        
        try {
            // 현재 등록된 모든 드라이버 로그 출력
            logRegisteredDrivers();
            
            // 가비지 컬렉션 실행으로 메모리 정리
            System.gc();
            
            // 잠시 대기
            Thread.sleep(2000);
            
            logger.info("JDBC 드라이버 정리 완료");
            
        } catch (Exception e) {
            logger.error("JDBC 드라이버 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 현재 등록된 모든 JDBC 드라이버 목록을 로그로 출력합니다.
     */
    public static void logRegisteredDrivers() {
        try {
            logger.info("현재 등록된 JDBC 드라이버 목록:");
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            
            int count = 0;
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                logger.info("  {}: {}", ++count, driver.getClass().getName());
            }
            
            if (count == 0) {
                logger.info("  등록된 JDBC 드라이버가 없습니다.");
            }
            
        } catch (Exception e) {
            logger.error("JDBC 드라이버 목록 조회 중 오류 발생", e);
        }
    }
    
    /**
     * 메모리 정리를 위한 가비지 컬렉션을 실행합니다.
     */
    public static void forceGarbageCollection() {
        try {
            logger.info("강제 가비지 컬렉션 실행...");
            
            // 여러 번 가비지 컬렉션 실행
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(1000);
            }
            
            logger.info("가비지 컬렉션 완료");
            
        } catch (Exception e) {
            logger.error("가비지 컬렉션 중 오류 발생", e);
        }
    }
} 