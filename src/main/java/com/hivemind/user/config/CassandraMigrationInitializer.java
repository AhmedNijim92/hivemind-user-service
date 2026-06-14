package com.hivemind.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

/**
 * Ensures new columns exist on Cassandra tables at startup.
 * This handles schema evolution without requiring manual ALTER TABLE commands.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CassandraMigrationInitializer implements CommandLineRunner
{
    private final CassandraOperations cassandraOperations;

    @Override
    public void run(String... args)
    {
        addColumnIfNotExists("user_profiles", "cover_picture_url", "text");
        addColumnIfNotExists("user_profiles", "show_contact_info", "boolean");
    }

    private void addColumnIfNotExists(String table, String column, String type)
    {
        try
        {
            cassandraOperations.getCqlOperations().execute(
                String.format("ALTER TABLE %s ADD %s %s", table, column, type)
            );
            log.info("Column {}.{} added successfully", table, column);
        }
        catch (Exception e)
        {
            // Column already exists — safe to ignore
            if (e.getMessage() != null && e.getMessage().contains("already exist"))
            {
                log.debug("Column {}.{} already exists", table, column);
            }
            else
            {
                log.warn("Could not add column {}.{}: {}", table, column, e.getMessage());
            }
        }
    }
}
