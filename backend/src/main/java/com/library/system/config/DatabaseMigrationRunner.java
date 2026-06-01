package com.library.system.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseMigrationRunner {

    private final DataSource dataSource;

    @PostConstruct
    public void runMigrations() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:db/migration/V*.sql");
            Arrays.sort(resources, (a, b) -> {
                String na = a.getFilename();
                String nb = b.getFilename();
                if (na == null) {
                    return 1;
                }
                if (nb == null) {
                    return -1;
                }
                return na.compareTo(nb);
            });

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                log.info("Running migration: {}", filename);
                try {
                    ScriptUtils.executeSqlScript(conn, resource);
                } catch (RuntimeException e) {
                    log.warn("Migration {} skipped (may already exist): {}", filename, e.getMessage());
                }
            }
            log.info("Database migration completed, processed {} scripts", resources.length);
        } catch (Exception e) {
            log.error("Database migration failed: {}", e.getMessage(), e);
        }
    }
}
