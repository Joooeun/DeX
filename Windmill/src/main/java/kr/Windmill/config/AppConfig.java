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
        JndiTemplate jndiTemplate = new JndiTemplate();
        DataSource dataSource = null;
        try {
           dataSource = (DataSource) jndiTemplate.lookup("java:comp/env/jdbc/appdb");
        } catch (NamingException e) {
            logger.error("Failed to lookup JNDI datasource", e);
        }
        return new DelegatingDataSource(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
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
