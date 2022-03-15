package dev.vality.hooker.dao;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.CleanTablesDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class CleanTablesDaoTest {

    @Autowired
    private CleanTablesDao cleanTablesDao;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void cleanInvocingTest() {

        String sql = "with queue as ( " +
                " insert into hook.invoicing_queue(hook_id, invoice_id, wtime) " +
                "values (1, :invoice_id, (now() at time zone 'utc' - (interval '1 days')*:days_ago )) returning *) " +
                "insert into hook.simple_retry_policy(queue_id, message_type) " +
                "select id, 'InvoicesTopic' from queue";

        int countOldRecords = 1000;
        IntStream.range(0, countOldRecords).forEach(i -> {
            jdbcTemplate.update(sql, new MapSqlParameterSource("invoice_id", "old" + i)
                    .addValue("days_ago", 11));
        });


        int countNewRecords = 500;
        IntStream.range(0, countNewRecords).forEach(i -> {
            jdbcTemplate.update(sql, new MapSqlParameterSource("invoice_id", "new" + i)
                    .addValue("days_ago", 8));
        });


        int count = cleanTablesDao.cleanInvocing(10);
        assertEquals(count, countOldRecords);

        long newRecordsCount = jdbcTemplate
                .queryForObject("select count(*) from hook.invoicing_queue", new MapSqlParameterSource(), Long.class);
        assertEquals(newRecordsCount, countNewRecords);
    }
}
