package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public HikariDataSource dataSource(@Value("${spring.datasource.url:}") String rawUrl,
                                       @Value("${spring.datasource.username:}") String username,
                                       @Value("${spring.datasource.password:}") String password) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        String user = username;
        String pass = password;

        if (url.isEmpty()) {
            throw new IllegalStateException("DB_URL is empty. Set it to your database address.");
        }

        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            URI uri = URI.create(url);
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            StringBuilder sb = new StringBuilder("jdbc:postgresql://")
                    .append(uri.getHost()).append(':').append(port).append(uri.getPath());
            if (uri.getRawQuery() != null) sb.append('?').append(uri.getRawQuery());
            if (uri.getRawUserInfo() != null) {
                String[] parts = uri.getRawUserInfo().split(":", 2);
                user = decode(parts[0]);
                if (parts.length > 1) pass = decode(parts[1]);
            }
            url = sb.toString();
        } else if (!url.startsWith("jdbc:")) {
            throw new IllegalStateException(
                    "DB_URL must start with jdbc:postgresql:// or postgresql:// (found: " + url.split("[?@]")[0] + ")");
        }

        if (System.getenv("RENDER") != null && url.contains("localhost")) {
            throw new IllegalStateException(
                    "DB_URL is missing or points to localhost. In Render > Environment set DB_URL to your "
                            + "database's Internal Database URL.");
        }
        if (url.contains(".render.com") && !url.contains("sslmode=")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setDriverClassName("org.postgresql.Driver");
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(20_000);

        log.info("Connecting to database: {}", url.replaceAll("\\?.*", ""));
        return new HikariDataSource(cfg);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}