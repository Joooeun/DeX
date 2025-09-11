package kr.Windmill.service;

import kr.Windmill.util.Common;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class SqlTemplateServiceTest {
    private JdbcTemplate jdbcTemplate;
    private SqlTemplateService service;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        service = new SqlTemplateService(new Common());
        Field f = SqlTemplateService.class.getDeclaredField("jdbcTemplate");
        f.setAccessible(true);
        f.set(service, jdbcTemplate);
    }

    @Test
    public void testGetSqlTemplateDetailIncludesParametersShortcutsAndAudit() {
        String templateId = "T1";

        Map<String, Object> mainRow = new HashMap<>();
        mainRow.put("TEMPLATE_ID", "T1");
        mainRow.put("TEMPLATE_NAME", "Test");
        mainRow.put("TEMPLATE_DESC", "desc");
        mainRow.put("SQL_CONTENT", "select 1");
        mainRow.put("ACCESSIBLE_CONNECTION_IDS", "DB1,DB2");
        mainRow.put("CHART_MAPPING", "chart");
        mainRow.put("VERSION", 1);
        mainRow.put("STATUS", "ACTIVE");
        mainRow.put("EXECUTION_LIMIT", 0);
        mainRow.put("REFRESH_TIMEOUT", 0);
        mainRow.put("NEWLINE", true);
        mainRow.put("AUDIT", true);
        List<Map<String, Object>> mainRows = Collections.singletonList(mainRow);

        Map<String, Object> param = new HashMap<>();
        param.put("PARAMETER_NAME", "p1");
        param.put("DEFAULT_VALUE", "v1");
        List<Map<String, Object>> paramRows = Collections.singletonList(param);

        Map<String, Object> shortcut = new HashMap<>();
        shortcut.put("SHORTCUT_KEY", "s1");
        shortcut.put("SHORTCUT_NAME", "name");
        shortcut.put("TARGET_TEMPLATE_ID", "T2");
        shortcut.put("SHORTCUT_DESCRIPTION", "desc");
        shortcut.put("SOURCE_COLUMN_INDEXES", "1");
        shortcut.put("AUTO_EXECUTE", true);
        shortcut.put("IS_ACTIVE", true);
        List<Map<String, Object>> shortcutRows = Collections.singletonList(shortcut);

        String detailSql = "SELECT TEMPLATE_ID, TEMPLATE_NAME, TEMPLATE_DESC, SQL_CONTENT, ACCESSIBLE_CONNECTION_IDS, CHART_MAPPING, VERSION, STATUS, EXECUTION_LIMIT, REFRESH_TIMEOUT, NEWLINE, AUDIT FROM SQL_TEMPLATE WHERE TEMPLATE_ID = ?";
        String paramSql = "SELECT PARAMETER_NAME, PARAMETER_TYPE, DEFAULT_VALUE, IS_REQUIRED, PARAMETER_ORDER, IS_READONLY, IS_HIDDEN, IS_DISABLED, DESCRIPTION FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ? ORDER BY PARAMETER_ORDER";
        String shortcutSql = "SELECT SHORTCUT_KEY, SHORTCUT_NAME, TARGET_TEMPLATE_ID, SHORTCUT_DESCRIPTION, SOURCE_COLUMN_INDEXES, AUTO_EXECUTE, IS_ACTIVE FROM SQL_TEMPLATE_SHORTCUT WHERE SOURCE_TEMPLATE_ID = ? ORDER BY SHORTCUT_KEY";
        String configSql = "SELECT PARAMETER_NAME, DEFAULT_VALUE FROM SQL_TEMPLATE_PARAMETER WHERE TEMPLATE_ID = ? ORDER BY PARAMETER_ORDER";

        Mockito.when(jdbcTemplate.queryForList(detailSql, templateId)).thenReturn(mainRows);
        Mockito.when(jdbcTemplate.queryForList(paramSql, templateId)).thenReturn(paramRows);
        Mockito.when(jdbcTemplate.queryForList(shortcutSql, templateId)).thenReturn(shortcutRows);
        Mockito.when(jdbcTemplate.queryForList(configSql, templateId)).thenReturn(paramRows);

        Map<String, Object> result = service.getSqlTemplateDetail(templateId);
        assertTrue((Boolean) result.get("success"));
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(Arrays.asList("DB1", "DB2"), data.get("accessibleConnectionIds"));
        assertEquals(true, data.get("audit"));
        assertEquals(1, ((List<?>) data.get("parameters")).size());
        assertEquals(1, ((List<?>) data.get("shortcuts")).size());
    }
}
