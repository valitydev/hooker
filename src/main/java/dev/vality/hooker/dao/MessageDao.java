package dev.vality.hooker.dao;

import dev.vality.hooker.exception.DaoException;

import java.util.Collection;
import java.util.List;

public interface MessageDao<M> {
    List<M> getBy(Collection<Long> messageIds) throws DaoException;
}
