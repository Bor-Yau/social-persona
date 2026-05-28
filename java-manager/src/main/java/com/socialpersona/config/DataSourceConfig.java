package com.socialpersona.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * SQLite Schema 自动初始化
 *
 * ★ SQLite JDBC 不支持 ; 分隔多语句。
 *   schema.sql 中的 DDL 需手动执行（首次部署时）。
 *   启动时跳过——避免 data/ 目录不存在导致启动失败。
 *
 * ★ 手动建表：
 *   sqlite3 data/social_persona.db < src/main/resources/schema.sql
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.schema-enabled:false}")
    private boolean schemaEnabled;

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);

        if (schemaEnabled) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.setContinueOnError(true);
            initializer.setDatabasePopulator(populator);
        }

        initializer.setEnabled(schemaEnabled);
        log.info("Schema 自动建表: {}", schemaEnabled ? "启用" : "禁用（手动执行 schema.sql）");
        return initializer;
    }
}
