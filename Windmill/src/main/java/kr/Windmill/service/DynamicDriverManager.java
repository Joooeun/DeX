package kr.Windmill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 동적 JAR 로딩을 통한 JDBC 드라이버 관리 시스템
 * 여러 버전의 데이터베이스 드라이버를 동시에 로드하고 관리
 */
@Service
public class DynamicDriverManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicDriverManager.class);
    
    // 로드된 드라이버들을 추적하는 맵
    private final Map<String, LoadedDriver> loadedDrivers = new ConcurrentHashMap<>();
    
    // 드라이버별 클래스로더 관리
    private final Map<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();
    
    /**
     * 로드된 드라이버 정보를 담는 내부 클래스
     */
    private static class LoadedDriver {
        private final String driverClassName;
        private final String jarPath;
        private final String version;
        private final URLClassLoader classLoader;
        private final Driver driver;
        
        public LoadedDriver(String driverClassName, String jarPath, String version, 
                          URLClassLoader classLoader, Driver driver) {
            this.driverClassName = driverClassName;
            this.jarPath = jarPath;
            this.version = version;
            this.classLoader = classLoader;
            this.driver = driver;
        }
        
        // Getters
        public String getDriverClassName() { return driverClassName; }
        public String getJarPath() { return jarPath; }
        public String getVersion() { return version; }
        public URLClassLoader getClassLoader() { return classLoader; }
        public Driver getDriver() { return driver; }
    }
    
    /**
     * 애플리케이션 시작 시 기본 드라이버들 로드
     */
    @PostConstruct
    public void initializeDefaultDrivers() {
        logger.info("기본 JDBC 드라이버 초기화 시작");
        
        try {
            // 기본 드라이버들 로드 (클래스패스에 있는 것들)
            loadDefaultDrivers();
            
            // 외부 JAR 파일에서 드라이버 로드
            loadExternalDrivers();
            
            logger.info("기본 JDBC 드라이버 초기화 완료");
        } catch (Exception e) {
            logger.error("기본 JDBC 드라이버 초기화 실패", e);
        }
    }
    
    /**
     * 기본 드라이버들 로드 (클래스패스에 있는 것들)
     */
    private void loadDefaultDrivers() {
        String[] defaultDrivers = {
            "com.ibm.db2.jcc.DB2Driver",
            "oracle.jdbc.driver.OracleDriver",
            "org.postgresql.Driver",
            "com.tmax.tibero.jdbc.TbDriver"
        };
        
        for (String driverClassName : defaultDrivers) {
            try {
                Class.forName(driverClassName);
                logger.info("기본 드라이버 로드 성공: {}", driverClassName);
            } catch (ClassNotFoundException e) {
                logger.debug("기본 드라이버를 찾을 수 없음: {}", driverClassName);
            }
        }
    }
    
    /**
     * 외부 JAR 파일에서 드라이버 로드
     */
    private void loadExternalDrivers() {
        // 드라이버 JAR 파일들이 저장된 디렉토리
        String driverLibPath = System.getProperty("user.home") + "/dex-drivers";
        File driverLibDir = new File(driverLibPath);
        
        if (!driverLibDir.exists()) {
            logger.info("드라이버 라이브러리 디렉토리가 존재하지 않음: {}", driverLibPath);
            return;
        }
        
        File[] jarFiles = driverLibDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        
        if (jarFiles == null || jarFiles.length == 0) {
            logger.info("드라이버 JAR 파일을 찾을 수 없음: {}", driverLibPath);
            return;
        }
        
        for (File jarFile : jarFiles) {
            try {
                loadDriverFromJar(jarFile);
            } catch (Exception e) {
                logger.error("JAR 파일에서 드라이버 로드 실패: {}", jarFile.getName(), e);
            }
        }
    }
    
    /**
     * JAR 파일에서 드라이버 로드
     */
    public void loadDriverFromJar(File jarFile) throws Exception {
        String jarPath = jarFile.getAbsolutePath();
        String jarName = jarFile.getName();
        
        logger.info("JAR 파일에서 드라이버 로드 시작: {}", jarName);
        
        // JAR 파일이 이미 로드되었는지 확인
        if (loadedDrivers.containsKey(jarPath)) {
            logger.info("이미 로드된 JAR 파일: {}", jarName);
            return;
        }
        
        // JAR 파일에서 드라이버 클래스명 추출
        String driverClassName = extractDriverClassName(jarFile);
        if (driverClassName == null) {
            logger.warn("JAR 파일에서 드라이버 클래스를 찾을 수 없음: {}", jarName);
            return;
        }
        
        // 버전 정보 추출
        String version = extractVersionFromJarName(jarName);
        
        // URLClassLoader 생성
        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{jarUrl}, 
            DynamicDriverManager.class.getClassLoader()
        );
        
        // 드라이버 클래스 로드
        Class<?> driverClass = classLoader.loadClass(driverClassName);
        
        // 드라이버 인스턴스 생성
        Driver driver = (Driver) driverClass.newInstance();
        
        // DriverManager에 등록
        DriverManager.registerDriver(new DriverWrapper(driver, classLoader));
        
        // 로드된 드라이버 정보 저장
        LoadedDriver loadedDriver = new LoadedDriver(driverClassName, jarPath, version, classLoader, driver);
        loadedDrivers.put(jarPath, loadedDriver);
        classLoaders.put(jarPath, classLoader);
        
        logger.info("드라이버 로드 성공: {} (버전: {})", driverClassName, version);
    }
    
    /**
     * JAR 파일명에서 버전 정보 추출
     */
    private String extractVersionFromJarName(String jarName) {
        // 예: db2jcc4-11.5.0.0.jar -> 11.5.0.0
        if (jarName.contains("-")) {
            String[] parts = jarName.split("-");
            if (parts.length > 1) {
                String versionPart = parts[parts.length - 1];
                return versionPart.replace(".jar", "");
            }
        }
        return "unknown";
    }
    
    /**
     * JAR 파일에서 드라이버 클래스명 추출
     */
    private String extractDriverClassName(File jarFile) {
        // 일반적인 드라이버 클래스명들
        String[] commonDrivers = {
            "com.ibm.db2.jcc.DB2Driver",
            "oracle.jdbc.driver.OracleDriver",
            "org.postgresql.Driver",
            "com.tmax.tibero.jdbc.TbDriver",
            "com.mysql.jdbc.Driver",
            "com.mysql.cj.jdbc.Driver"
        };
        
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            for (String driverClassName : commonDrivers) {
                String classPath = driverClassName.replace('.', '/') + ".class";
                if (jar.getEntry(classPath) != null) {
                    return driverClassName;
                }
            }
        } catch (Exception e) {
            logger.error("JAR 파일 분석 실패: {}", jarFile.getName(), e);
        }
        
        return null;
    }
    
    /**
     * 특정 드라이버가 로드되었는지 확인
     */
    public boolean isDriverLoaded(String driverClassName) {
        try {
            Class.forName(driverClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 로드된 드라이버 목록 조회
     */
    public Map<String, String> getLoadedDrivers() {
        Map<String, String> result = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, LoadedDriver> entry : loadedDrivers.entrySet()) {
            LoadedDriver driver = entry.getValue();
            result.put(driver.getDriverClassName(), driver.getVersion());
        }
        
        return result;
    }
    
    /**
     * 특정 JAR 파일의 드라이버 언로드
     */
    public void unloadDriver(String jarPath) {
        LoadedDriver loadedDriver = loadedDrivers.remove(jarPath);
        if (loadedDriver != null) {
            try {
                // DriverManager에서 제거 (Java 8에서는 deregister 메서드가 없음)
                // 대신 클래스로더를 닫아서 드라이버를 비활성화
                
                // 클래스로더 닫기
                URLClassLoader classLoader = classLoaders.remove(jarPath);
                if (classLoader != null) {
                    classLoader.close();
                }
                
                logger.info("드라이버 언로드 완료: {}", jarPath);
            } catch (Exception e) {
                logger.error("드라이버 언로드 실패: {}", jarPath, e);
            }
        }
    }
    
    /**
     * 애플리케이션 종료 시 정리
     */
    @PreDestroy
    public void cleanup() {
        logger.info("동적 드라이버 매니저 정리 시작");
        
        // 모든 로드된 드라이버 언로드
        for (String jarPath : loadedDrivers.keySet()) {
            unloadDriver(jarPath);
        }
        
        // 클래스로더들 정리
        for (URLClassLoader classLoader : classLoaders.values()) {
            try {
                classLoader.close();
            } catch (Exception e) {
                logger.error("클래스로더 정리 실패", e);
            }
        }
        
        classLoaders.clear();
        loadedDrivers.clear();
        
        logger.info("동적 드라이버 매니저 정리 완료");
    }
    
    /**
     * Driver 래퍼 클래스 (클래스로더 정보 보존)
     */
    private static class DriverWrapper implements Driver {
        private final Driver delegate;
        private final URLClassLoader classLoader;
        
        public DriverWrapper(Driver delegate, URLClassLoader classLoader) {
            this.delegate = delegate;
            this.classLoader = classLoader;
        }
        
        @Override
        public java.sql.Connection connect(String url, java.util.Properties info) throws java.sql.SQLException {
            return delegate.connect(url, info);
        }
        
        @Override
        public boolean acceptsURL(String url) throws java.sql.SQLException {
            return delegate.acceptsURL(url);
        }
        
        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws java.sql.SQLException {
            return delegate.getPropertyInfo(url, info);
        }
        
        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }
        
        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }
        
        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }
        
        @Override
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
