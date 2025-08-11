package kr.Windmill.config;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;



@Configuration
@ComponentScan( basePackages = { "kr.Windmill" },
                excludeFilters = @Filter({ org.springframework.stereotype.Controller.class }))
@EnableTransactionManagement
@MapperScan("kr.Windmill.mapper")
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    
    @Bean
    public DataSource dataSource() {
        // JNDI 데이터소스 사용
        JndiTemplate jndiTemplate = new JndiTemplate();
        try {
           DataSource dataSource = (DataSource) jndiTemplate.lookup("java:comp/env/jdbc/appdb");
           logger.info("JNDI 데이터소스 사용: java:comp/env/jdbc/appdb");
           return new DelegatingDataSource(dataSource);
        } catch (NamingException e) {
            logger.error("JNDI 데이터소스를 찾을 수 없습니다: {}", e.getMessage());
            throw new RuntimeException("데이터소스 설정이 필요합니다. context.xml을 확인해주세요.", e);
        }
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory() {
        
        org.apache.ibatis.session.Configuration mybatisConfig = new org.apache.ibatis.session.Configuration();
        mybatisConfig.setMapUnderscoreToCamelCase(true);        
        
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setTypeAliasesPackage("kr.Windmill.vo");
        sessionFactoryBean.setConfiguration(mybatisConfig);        
        return sessionFactoryBean;
    }

}
