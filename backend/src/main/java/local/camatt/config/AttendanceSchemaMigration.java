package local.camatt.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class AttendanceSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public AttendanceSchemaMigration(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql")) return;
        }

        // Older MVP versions allowed only one row per employee/date. Remove that
        // generated unique index once so each visit can be stored as a session.
        List<String> indexes = jdbc.queryForList("""
            SELECT INDEX_NAME
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'attendance_records'
              AND NON_UNIQUE = 0
              AND INDEX_NAME <> 'PRIMARY'
            GROUP BY INDEX_NAME
            HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'employee_id,attendance_date'
            """, String.class);
        for (String index : indexes) {
            jdbc.execute("ALTER TABLE attendance_records DROP INDEX `" + index.replace("`", "``") + "`");
        }
    }
}
