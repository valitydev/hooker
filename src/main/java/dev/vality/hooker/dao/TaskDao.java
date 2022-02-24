package dev.vality.hooker.dao;


import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.Task;

import java.util.List;
import java.util.Map;

/**
 * Created by jeckep on 13.04.17.
 */
public interface TaskDao {
    void remove(long queueId, long messageId);

    void removeAll(long queueId) throws DaoException;

    Map<Long, List<Task>> getScheduled() throws DaoException;
}
