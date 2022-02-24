package dev.vality.hooker.dao;

import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.retry.impl.simple.SimpleRetryPolicyRecord;

/**
 * Created by jeckep on 17.04.17.
 */
public interface SimpleRetryPolicyDao {
    void update(SimpleRetryPolicyRecord record) throws DaoException;
}
