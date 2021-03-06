package dev.vality.hooker.dao.rowmapper;

import dev.vality.hooker.model.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class InvoicingRowMapper implements RowMapper<InvoicingMessage> {

    public static final String ID = "id";
    public static final String EVENT_TIME = "event_time";
    public static final String SEQUENCE_ID = "sequence_id";
    public static final String CHANGE_ID = "change_id";
    public static final String TYPE = "type";
    public static final String PARTY_ID = "party_id";
    public static final String EVENT_TYPE = "event_type";
    public static final String INVOICE_ID = "invoice_id";
    public static final String SHOP_ID = "shop_id";
    public static final String INVOICE_STATUS = "invoice_status";
    public static final String PAYMENT_ID = "payment_id";
    public static final String PAYMENT_STATUS = "payment_status";
    public static final String REFUND_ID = "refund_id";
    public static final String REFUND_STATUS = "refund_status";

    @Override
    public InvoicingMessage mapRow(ResultSet rs, int i) throws SQLException {
        InvoicingMessage message = new InvoicingMessage();
        message.setId(rs.getLong(ID));
        message.setEventTime(rs.getString(EVENT_TIME));
        message.setSequenceId(rs.getLong(SEQUENCE_ID));
        message.setChangeId(rs.getInt(CHANGE_ID));
        message.setType(InvoicingMessageEnum.lookup(rs.getString(TYPE)));
        message.setPartyId(rs.getString(PARTY_ID));
        message.setShopId(rs.getString(SHOP_ID));
        message.setEventType(EventType.valueOf(rs.getString(EVENT_TYPE)));
        message.setSourceId(rs.getString(INVOICE_ID));
        message.setInvoiceStatus(InvoiceStatusEnum.lookup(rs.getString(INVOICE_STATUS)));
        message.setPaymentId(rs.getString(PAYMENT_ID));
        message.setPaymentStatus(rs.getString(PAYMENT_STATUS) != null
                ? PaymentStatusEnum.lookup(rs.getString(PAYMENT_STATUS)) : null);
        message.setRefundId(rs.getString(REFUND_ID));
        message.setRefundStatus(rs.getString(REFUND_STATUS) != null
                ? RefundStatusEnum.lookup(rs.getString(REFUND_STATUS)) : null);
        return message;
    }
}
