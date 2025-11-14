package kr.Windmill.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 애플리케이션 버전 정보를 관리하는 유틸리티 클래스
 */
public class VersionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(VersionUtil.class);
    private static final String VERSION_PROPERTIES = "version.properties";
    
    private static Properties versionProperties;
    
    static {
        loadVersionProperties();
    }
    
    /**
     * 버전 프로퍼티 파일을 로드합니다.
     */
    private static void loadVersionProperties() {
        versionProperties = new Properties();
        try (InputStream inputStream = VersionUtil.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES)) {
            if (inputStream != null) {
                versionProperties.load(inputStream);
                logger.info("버전 정보 로드 완료: {}", getVersion());
            } else {
                logger.warn("버전 프로퍼티 파일을 찾을 수 없습니다: {}", VERSION_PROPERTIES);
            }
        } catch (IOException e) {
            logger.error("버전 프로퍼티 파일 로드 중 오류 발생", e);
        }
    }
    
    /**
     * 애플리케이션 버전을 반환합니다.
     * @return 버전 문자열
     */
    public static String getVersion() {
        return versionProperties.getProperty("app.version", "2.1.1");
    }
    

} 