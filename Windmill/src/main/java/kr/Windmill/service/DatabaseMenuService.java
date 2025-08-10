package kr.Windmill.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import kr.Windmill.util.Common;

@Service
public class DatabaseMenuService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMenuService.class);
    
    @Autowired
    private Common com;
    
    /**
     * 전체 메뉴 트리 조회 (관리자용)
     */
    public List<Map<String, Object>> getFullMenuTree() {
        try {
            // 현재는 파일 기반으로 동작하도록 임시 구현
            // DB 이전 후에는 실제 DB 조회로 변경
            return getFileBasedMenuTree(null);
        } catch (Exception e) {
            logger.error("전체 메뉴 트리 조회 실패", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 사용자별 메뉴 트리 조회
     */
    public List<Map<String, Object>> getUserMenuTree(String userId) {
        try {
            // 현재는 파일 기반으로 동작하도록 임시 구현
            // DB 이전 후에는 실제 DB 조회로 변경
            return getFileBasedMenuTree(userId);
        } catch (Exception e) {
            logger.error("사용자 메뉴 트리 조회 실패", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * SQL 템플릿 상세 조회
     */
    public Map<String, Object> getSqlTemplateDetail(String sqlId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 현재는 파일 기반으로 동작하도록 임시 구현
            // DB 이전 후에는 실제 DB 조회로 변경
            if (sqlId == null || sqlId.trim().isEmpty()) {
                result.put("error", "SQL ID가 지정되지 않았습니다.");
                return result;
            }
            
            // 파일 경로에서 SQL 파일과 Properties 파일 읽기
            // 먼저 정확한 경로를 찾기 위해 파일 검색
            String sqlPath = findSqlFile(sqlId + ".sql");
            String propertiesPath = findSqlFile(sqlId + ".properties");
            
            if (sqlPath == null) {
                result.put("error", "SQL 파일을 찾을 수 없습니다: " + sqlId + ".sql");
                return result;
            }
            
            String sqlContent = com.FileRead(new java.io.File(sqlPath));
            String propertiesContent = com.FileRead(new java.io.File(propertiesPath));
            
            // 파일 경로에서 상대 경로 추출
            String relativePath = "";
            if (sqlPath != null && sqlPath.startsWith(com.SrcPath)) {
                relativePath = sqlPath.substring(com.SrcPath.length());
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                // 파일명 제거하고 폴더 경로만
                int lastSlash = relativePath.lastIndexOf("/");
                if (lastSlash > 0) {
                    relativePath = relativePath.substring(0, lastSlash);
                } else {
                    relativePath = "";
                }
            }
            
            result.put("menuId", sqlId);
            result.put("menuName", sqlId);
            result.put("menuPath", relativePath);
            result.put("sqlContent", sqlContent);
            result.put("config", parseProperties(propertiesContent));
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 상세 조회 실패", e);
            result.put("error", "SQL 템플릿 조회 실패: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * SQL 템플릿 저장
     */
    public Map<String, Object> saveSqlTemplate(String sqlId, String sqlName, String sqlPath, 
                                              String sqlContent, String configContent, String userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 현재는 파일 기반으로 동작하도록 임시 구현
            // DB 이전 후에는 실제 DB 저장으로 변경
            
            if (sqlName == null || sqlName.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "SQL 이름을 입력해주세요.");
                return result;
            }
            
            if (sqlContent == null || sqlContent.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "SQL 내용을 입력해주세요.");
                return result;
            }
            
            // 새로 생성하는 경우
            if (sqlId == null || sqlId.trim().isEmpty()) {
                sqlId = generateSqlId(sqlName);
            }
            
            // 파일 저장 - 폴더 구조 유지
            String finalSqlPath;
            String finalPropertiesPath;
            
            if (sqlPath != null && !sqlPath.trim().isEmpty()) {
                // 기존 경로가 있는 경우 해당 경로에 저장
                finalSqlPath = com.SrcPath + sqlPath + "/" + sqlId + ".sql";
                finalPropertiesPath = com.SrcPath + sqlPath + "/" + sqlId + ".properties";
            } else {
                // 새로 생성하는 경우 기본 경로에 저장
                finalSqlPath = com.SrcPath + sqlId + ".sql";
                finalPropertiesPath = com.SrcPath + sqlId + ".properties";
            }
            
            // 폴더가 없으면 생성
            java.io.File sqlFile = new java.io.File(finalSqlPath);
            java.io.File parentDir = sqlFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // SQL 파일 저장
            java.io.FileWriter sqlWriter = new java.io.FileWriter(finalSqlPath);
            sqlWriter.write(sqlContent);
            sqlWriter.close();
            
            // Properties 파일 저장
            java.io.FileWriter propWriter = new java.io.FileWriter(finalPropertiesPath);
            propWriter.write(configContent);
            propWriter.close();
            
            result.put("success", true);
            result.put("sqlId", sqlId);
            result.put("message", "SQL 템플릿이 저장되었습니다.");
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 저장 실패", e);
            result.put("success", false);
            result.put("error", "SQL 템플릿 저장 실패: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * SQL 템플릿 삭제
     */
    public Map<String, Object> deleteSqlTemplate(String sqlId, String userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 현재는 파일 기반으로 동작하도록 임시 구현
            // DB 이전 후에는 실제 DB 삭제로 변경
            
            if (sqlId == null || sqlId.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "SQL ID가 지정되지 않았습니다.");
                return result;
            }
            
            // 파일 삭제
            String sqlPath = com.SrcPath + sqlId + ".sql";
            String propertiesPath = com.SrcPath + sqlId + ".properties";
            
            java.io.File sqlFile = new java.io.File(sqlPath);
            java.io.File propertiesFile = new java.io.File(propertiesPath);
            
            if (sqlFile.exists()) {
                sqlFile.delete();
            }
            
            if (propertiesFile.exists()) {
                propertiesFile.delete();
            }
            
            result.put("success", true);
            result.put("message", "SQL 템플릿이 삭제되었습니다.");
            
        } catch (Exception e) {
            logger.error("SQL 템플릿 삭제 실패", e);
            result.put("success", false);
            result.put("error", "SQL 템플릿 삭제 실패: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 파일 기반 메뉴 트리 조회 (임시 구현)
     */
    private List<Map<String, Object>> getFileBasedMenuTree(String userId) {
        try {
            List<Map<String, ?>> fileList = com.getfiles(com.SrcPath, 0);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Map<String, ?> item : fileList) {
                Map<String, Object> node = new HashMap<>();
                
                if (item.get("Path").toString().contains("Path")) {
                    // 폴더인 경우
                    node.put("type", "folder");
                    node.put("id", item.get("Name"));
                    node.put("name", item.get("Name"));
                    node.put("children", convertFileListToTree((List<Map<String, ?>>) item.get("list"), userId));
                } else {
                    // 파일인 경우
                    String fileName = item.get("Name").toString();
                    if (fileName.endsWith(".sql")) {
                        node.put("type", "sql");
                        // 파일명에서 .sql 확장자 제거하여 ID 생성
                        String sqlId = fileName.replace(".sql", "");
                        node.put("id", sqlId);
                        node.put("name", fileName);
                        node.put("path", item.get("Path").toString());
                    }
                }
                
                if (node.containsKey("type")) {
                    result.add(node);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("파일 기반 메뉴 트리 조회 실패", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 파일 리스트를 트리 구조로 변환
     */
    private List<Map<String, Object>> convertFileListToTree(List<Map<String, ?>> fileList, String userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (fileList == null) {
            return result;
        }
        
        for (Map<String, ?> item : fileList) {
            Map<String, Object> node = new HashMap<>();
            
            if (item.get("Path").toString().contains("Path")) {
                // 폴더인 경우
                node.put("type", "folder");
                node.put("id", item.get("Name"));
                node.put("name", item.get("Name"));
                node.put("children", convertFileListToTree((List<Map<String, ?>>) item.get("list"), userId));
            } else {
                // 파일인 경우
                String fileName = item.get("Name").toString();
                if (fileName.endsWith(".sql")) {
                    node.put("type", "sql");
                    node.put("id", fileName.replace(".sql", ""));
                    node.put("name", fileName);
                    node.put("path", item.get("Path").toString());
                }
            }
            
            if (node.containsKey("type")) {
                result.add(node);
            }
        }
        
        return result;
    }
    
    /**
     * Properties 문자열을 Map으로 파싱
     */
    private Map<String, Object> parseProperties(String propertiesContent) {
        Map<String, Object> result = new HashMap<>();
        
        if (propertiesContent == null || propertiesContent.trim().isEmpty()) {
            return result;
        }
        
        String[] lines = propertiesContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    result.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        return result;
    }
    
    /**
     * SQL 파일 경로 찾기
     */
    private String findSqlFile(String fileName) {
        try {
            java.io.File srcDir = new java.io.File(com.SrcPath);
            if (!srcDir.exists()) {
                return null;
            }
            
            // 재귀적으로 파일 검색
            return findFileRecursively(srcDir, fileName);
        } catch (Exception e) {
            logger.error("SQL 파일 검색 실패: " + fileName, e);
            return null;
        }
    }
    
    /**
     * 재귀적으로 파일 검색
     */
    private String findFileRecursively(java.io.File directory, String fileName) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }
        
        java.io.File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        
        for (java.io.File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return file.getAbsolutePath();
            } else if (file.isDirectory()) {
                String result = findFileRecursively(file, fileName);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * SQL ID 생성
     */
    private String generateSqlId(String sqlName) {
        // 파일명에서 특수문자 제거하고 ID 생성
        String cleanName = sqlName.replaceAll("[^a-zA-Z0-9_]", "_");
        return cleanName + "_" + System.currentTimeMillis();
    }
}
