package dev.vality.hooker.dao;

import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jeckep on 17.04.17.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTaskDao implements TaskDao {

    public static RowMapper<Task> taskRowMapper = (rs, i) ->
            new Task(rs.getLong("message_id"), rs.getLong("queue_id"));
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected abstract String getMessageTopic();

    @Override
    public void remove(long queueId, long messageId) throws DaoException {
        final String sql =
                "DELETE FROM hook.scheduled_task " +
                        " where queue_id=:queue_id " +
                        " and message_id=:message_id " +
                        " and message_type=CAST(:message_type as hook.message_topic)";
        try {
            jdbcTemplate.update(sql, new MapSqlParameterSource("queue_id", queueId)
                    .addValue("message_id", messageId)
                    .addValue("message_type", getMessageTopic()));
            log.debug("Task with queueId {} messageId  {} removed from hook.scheduled_task", queueId, messageId);
        } catch (NestedRuntimeException e) {
            log.warn("Fail to delete task by queue_id {} and message_id {}", queueId, messageId, e);
            throw new DaoException(e);
        }
    }

    @Override
    public void removeAll(long queueId) throws DaoException {
        final String sql = "DELETE FROM hook.scheduled_task " +
                "where queue_id=:queue_id and message_type=CAST(:message_type as hook.message_topic)";
        try {
            jdbcTemplate.update(sql,
                    new MapSqlParameterSource("queue_id", queueId).addValue("message_type", getMessageTopic()));
        } catch (NestedRuntimeException e) {
            log.warn("Fail to delete tasks for hook:" + queueId, e);
            throw new DaoException(e);
        }
    }

    //should preserve order by message id
    protected Map<Long, List<Task>> splitByQueue(List<Task> orderedByMessageIdTasks) {
        return orderedByMessageIdTasks.stream().collect(Collectors.groupingBy(Task::getQueueId));
    }
}
