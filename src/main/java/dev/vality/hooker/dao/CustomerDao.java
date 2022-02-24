package dev.vality.hooker.dao;

import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;

public interface CustomerDao extends MessageDao<CustomerMessage> {
    CustomerMessage getAny(String customerId, CustomerMessageEnum type) throws DaoException;
}
