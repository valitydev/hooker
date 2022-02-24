package dev.vality.hooker.dao;

import dev.vality.hooker.model.Queue;

import java.util.Collection;
import java.util.List;

/**
 * Created by inalarsanukaev on 14.11.17.
 */
public interface QueueDao<Q extends Queue> {
    List<Q> getWithPolicies(Collection<Long> ids);

    void disable(long id);
}
