package com.lifelink.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Production datasource. Understands the two ways hosts hand out a database:
 *  - DATABASE_URL = postgres://user:pass@host:5432/db   (Render, Railway, Heroku style)
 *  - DB_URL = jdbc:postgresql://host:5432/db with DB_USERNAME / DB_PASSWORD  (Neon, Supabase, manual)
 * If neither is set it falls back to a file-based H2 database so the app still starts.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    @Bean
    public DataSource dataSource(
            @Value("${DATABASE_URL:}") String databaseUrl,
            @Value("${DB_URL:}") String jdbcUrl,
            @Value("${DB_USERNAME:}") String username,
            @Value("${DB_PASSWORD:}") String password) throws Exception {

        HikariConfig cfg = new HikariConfig();
        cfg.setMaximumPoolSize(5);

        if (!databaseUrl.isBlank()) {
            URI uri = new URI(databaseUrl.replaceFirst("^postgresql://", "postgres://"));
            String[] userInfo = uri.getUserInfo() == null ? new String[]{"", ""} : uri.getUserInfo().split(":", 2);
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
            cfg.setJdbcUrl("jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + query);
            cfg.setUsername(userInfo[0]);
            cfg.setPassword(userInfo.length > 1 ? userInfo[1] : "");
            log.info("Using PostgreSQL from DATABASE_URL at host {}", uri.getHost());
        } else if (!jdbcUrl.isBlank()) {
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(username);
            cfg.setPassword(password);
            log.info("Using database from DB_URL");
        } else {
            cfg.setJdbcUrl("jdbc:h2:file:./data/lifelink");
            cfg.setUsername("sa");
            cfg.setPassword("");
            log.warn("No DATABASE_URL or DB_URL set - falling back to a local H2 file database (not for real use)");
        }
        return new HikariDataSource(cfg);
    }
}
