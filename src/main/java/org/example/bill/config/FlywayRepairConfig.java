package org.example.bill.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 当本地迁移脚本被调整（合并版本、改内容）后，已执行过的版本在 flyway_schema_history 中的 checksum
 * 会与当前文件不一致。可先设 {@code app.flyway.repair-before-migrate=true} 启动一次以执行 repair，
 * 或在外部运行 {@code mvn flyway:repair}。
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${app.flyway.repair-before-migrate:false}") boolean repairBeforeMigrate) {
        return (Flyway flyway) -> {
            if (repairBeforeMigrate) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
