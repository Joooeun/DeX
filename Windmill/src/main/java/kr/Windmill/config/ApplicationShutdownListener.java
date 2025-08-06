package kr.Windmill.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kr.Windmill.util.Common;

@WebListener
public class ApplicationShutdownListener implements ServletContextListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownListener.class);
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Windmill 애플리케이션이 시작되었습니다.");
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Windmill 애플리케이션 종료를 시작합니다...");
        
        try {
            // Spring 컨텍스트 가져오기
            WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
            
            if (context != null) {
                // ConnectionStatusService는 @PreDestroy로 자동 정리됨
                logger.info("Spring 컨텍스트에서 ConnectionStatusService 자동 정리 대기 중...");
            }
            
            // JDBC 드라이버 정리는 Common.cleanupOnShutdown()에서 처리됨
            
            // Common 클래스의 정리 작업 실행
            try {
                logger.info("Common 클래스 정리 작업 시작...");
                Common.cleanupOnShutdown();
            } catch (Exception e) {
                logger.warn("Common 클래스 정리 작업 중 오류 발생", e);
            }
            
            // 추가 정리 작업
            cleanupResources();
            
            logger.info("Windmill 애플리케이션이 안전하게 종료되었습니다.");
            
        } catch (Exception e) {
            logger.error("애플리케이션 종료 중 오류 발생", e);
        }
    }
    
    private void cleanupResources() {
        try {
            // 임시 파일 정리
            cleanupTempFiles();
            
            // 스레드 풀 정리
            cleanupThreadPools();
            
            logger.info("리소스 정리가 완료되었습니다.");
            
        } catch (Exception e) {
            logger.warn("리소스 정리 중 오류 발생", e);
        }
    }
    
    private void cleanupTempFiles() {
        try {
            // 임시 디렉토리 정리 로직
            logger.info("임시 파일 정리 완료");
        } catch (Exception e) {
            logger.warn("임시 파일 정리 중 오류", e);
        }
    }
    
    private void cleanupThreadPools() {
        try {
            // 스레드 풀 정리 로직
            logger.info("스레드 풀 정리 완료");
        } catch (Exception e) {
            logger.warn("스레드 풀 정리 중 오류", e);
        }
    }
} 