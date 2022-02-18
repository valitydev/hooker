package dev.vality.hooker.dao.rowmapper;

import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CustomerRowMapper implements RowMapper<CustomerMessage> {
    public static final String ID = "id";
    public static final String EVENT_ID = "event_id";
    public static final String TYPE = "type";
    public static final String OCCURED_AT = "occured_at";
    public static final String SEQUENCE_ID = "sequence_id";
    public static final String CHANGE_ID = "change_id";
    public static final String PARTY_ID = "party_id";
    public static final String EVENT_TYPE = "event_type";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String CUSTOMER_SHOP_ID = "customer_shop_id";
    public static final String BINDING_ID = "binding_id";

    @Override
    public CustomerMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        CustomerMessage message = new CustomerMessage();
        message.setId(rs.getLong(ID));
        message.setEventId(rs.getLong(EVENT_ID));
        message.setPartyId(rs.getString(PARTY_ID));
        message.setEventTime(rs.getString(OCCURED_AT));
        message.setSequenceId(rs.getLong(SEQUENCE_ID));
        message.setChangeId(rs.getInt(CHANGE_ID));
        message.setType(CustomerMessageEnum.lookup(rs.getString(TYPE)));
        message.setEventType(EventType.valueOf(rs.getString(EVENT_TYPE)));
        message.setSourceId(rs.getString(CUSTOMER_ID));
        message.setShopId(rs.getString(CUSTOMER_SHOP_ID));
        message.setBindingId(rs.getString(BINDING_ID));
        return message;
    }
}
