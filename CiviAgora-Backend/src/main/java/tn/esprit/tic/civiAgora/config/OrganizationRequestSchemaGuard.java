package tn.esprit.tic.civiAgora.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationRequestSchemaGuard implements ApplicationRunner {

    private static final String TABLE_NAME = "organization_requests";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySqlCompatibleDatabase()) {
            return;
        }

        normalizeStatusColumn("request_status");
        normalizeStatusColumn("quote_status");
        normalizeStatusColumn("payment_status");
    }

    private void normalizeStatusColumn(String columnName) {
        if (!columnExists(columnName)) {
            return;
        }

        ColumnDefinition definition = getColumnDefinition(columnName);
        if (definition == null || definition.isCompatibleVarchar()) {
            return;
        }

        log.info(
                "Normalizing {}.{} from {} to VARCHAR(32) for extensible onboarding lifecycle statuses",
                TABLE_NAME,
                columnName,
                definition.columnType()
        );
        jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " MODIFY COLUMN " + columnName + " VARCHAR(32)");
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                Integer.class,
                TABLE_NAME,
                columnName
        );
        return count != null && count > 0;
    }

    private ColumnDefinition getColumnDefinition(String columnName) {
        return jdbcTemplate.query(
                """
                        SELECT data_type, character_maximum_length, column_type
                        FROM information_schema.columns
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                resultSet -> resultSet.next()
                        ? new ColumnDefinition(
                        resultSet.getString("data_type"),
                        resultSet.getInt("character_maximum_length"),
                        resultSet.getString("column_type")
                )
                        : null,
                TABLE_NAME,
                columnName
        );
    }

    private boolean isMySqlCompatibleDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            String normalizedName = databaseProductName == null
                    ? ""
                    : databaseProductName.toLowerCase(Locale.ROOT);
            return normalizedName.contains("mysql") || normalizedName.contains("mariadb");
        } catch (SQLException exception) {
            log.warn("Could not inspect database product for organization request schema guard", exception);
            return false;
        }
    }

    private record ColumnDefinition(String dataType, int characterMaximumLength, String columnType) {
        boolean isCompatibleVarchar() {
            return "varchar".equalsIgnoreCase(dataType) && characterMaximumLength >= 32;
        }
    }
}
