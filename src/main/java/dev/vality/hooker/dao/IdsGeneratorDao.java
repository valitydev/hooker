package dev.vality.hooker.dao;

import dev.vality.hooker.exception.DaoException;

import java.util.List;

public interface IdsGeneratorDao {
    List<Long> get(int size) throws DaoException;
}
