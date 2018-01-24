package com.rbkmoney.hooker.dao.impl;

import com.rbkmoney.hooker.dao.CacheMng;
import com.rbkmoney.hooker.dao.DaoException;
import com.rbkmoney.hooker.dao.InvoicingMessageDao;
import com.rbkmoney.hooker.model.*;
import com.rbkmoney.hooker.model.Invoice;
import com.rbkmoney.hooker.model.Payment;
import com.rbkmoney.hooker.model.PaymentContactInfo;
import com.rbkmoney.hooker.utils.PaymentToolUtils;
import com.rbkmoney.swag_webhook_events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;

import static com.rbkmoney.hooker.utils.PaymentToolUtils.getPaymentToolDetails;

public class InvoicingMessageDaoImpl extends NamedParameterJdbcDaoSupport implements InvoicingMessageDao {
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CacheMng cacheMng;

    @Autowired
    InvoicingQueueDao queueDao;

    @Autowired
    InvoicingTaskDao taskDao;

    public static final String ID = "id";
    public static final String EVENT_ID = "event_id";
    public static final String EVENT_TIME = "event_time";
    public static final String TYPE = "type";
    public static final String PARTY_ID = "party_id";
    public static final String EVENT_TYPE = "event_type";
    public static final String INVOICE_ID = "invoice_id";
    public static final String SHOP_ID = "shop_id";
    public static final String INVOICE_CREATED_AT = "invoice_created_at";
    public static final String INVOICE_STATUS = "invoice_status";
    public static final String INVOICE_REASON = "invoice_reason";
    public static final String INVOICE_DUE_DATE = "invoice_due_date";
    public static final String INVOICE_AMOUNT = "invoice_amount";
    public static final String INVOICE_CURRENCY = "invoice_currency";
    public static final String INVOICE_CONTENT_TYPE = "invoice_content_type";
    public static final String INVOICE_CONTENT_DATA = "invoice_content_data";
    public static final String INVOICE_PRODUCT = "invoice_product";
    public static final String INVOICE_DESCRIPTION = "invoice_description";
    public static final String PAYMENT_ID = "payment_id";
    public static final String PAYMENT_CREATED_AT = "payment_created_at";
    public static final String PAYMENT_STATUS = "payment_status";
    public static final String PAYMENT_ERROR_CODE = "payment_error_code";
    public static final String PAYMENT_ERROR_MESSAGE = "payment_error_message";
    public static final String PAYMENT_AMOUNT = "payment_amount";
    public static final String PAYMENT_CURRENCY = "payment_currency";
    public static final String PAYMENT_TOOL_TOKEN = "payment_tool_token";
    public static final String PAYMENT_SESSION = "payment_session";
    public static final String PAYMENT_EMAIL = "payment_email";
    public static final String PAYMENT_PHONE = "payment_phone";
    public static final String PAYMENT_IP = "payment_ip";
    public static final String PAYMENT_FINGERPRINT = "payment_fingerprint";
    public static final String PAYMENT_CUSTOMER_ID = "payment_customer_id";
    public static final String PAYMENT_PAYER_TYPE = "payment_payer_type";
    public static final String PAYMENT_TOOL_DETAILS_TYPE = "payment_tool_details_type";
    public static final String PAYMENT_CARD_NUMBER_MASK = "payment_card_number_mask";
    public static final String PAYMENT_SYSTEM = "payment_system";
    public static final String PAYMENT_TERMINAL_PROVIDER = "payment_terminal_provider";
    public static final String PAYMENT_DIGITAL_WALLET_PROVIDER = "payment_digital_wallet_provider";
    public static final String PAYMENT_DIGITAL_WALLET_ID = "payment_digital_wallet_id";

    //TODO refactoring
    private static void setNullPaymentParamValues(MapSqlParameterSource params) {
        params.addValue(PAYMENT_ID, null)
                .addValue(PAYMENT_CREATED_AT, null)
                .addValue(PAYMENT_STATUS, null)
                .addValue(PAYMENT_ERROR_CODE, null)
                .addValue(PAYMENT_ERROR_MESSAGE, null)
                .addValue(PAYMENT_AMOUNT, null)
                .addValue(PAYMENT_CURRENCY, null)
                .addValue(PAYMENT_TOOL_TOKEN, null)
                .addValue(PAYMENT_SESSION, null)
                .addValue(PAYMENT_EMAIL, null)
                .addValue(PAYMENT_PHONE, null)
                .addValue(PAYMENT_IP, null)
                .addValue(PAYMENT_FINGERPRINT, null)
                .addValue(PAYMENT_CUSTOMER_ID, null)
                .addValue(PAYMENT_PAYER_TYPE, null)
                .addValue(PAYMENT_TOOL_DETAILS_TYPE, null)
                .addValue(PAYMENT_CARD_NUMBER_MASK, null)
                .addValue(PAYMENT_SYSTEM, null)
                .addValue(PAYMENT_TERMINAL_PROVIDER, null)
                .addValue(PAYMENT_DIGITAL_WALLET_PROVIDER, null)
                .addValue(PAYMENT_DIGITAL_WALLET_ID, null);
    }

    private static RowMapper<InvoiceCartPosition> cartPositionRowMapper = (rs, i) -> {
        InvoiceCartPosition invoiceCartPosition = new InvoiceCartPosition();
        invoiceCartPosition.setProduct(rs.getString("product"));
        invoiceCartPosition.setPrice(rs.getLong("price"));
        invoiceCartPosition.setQuantity(rs.getInt("quantity"));
        invoiceCartPosition.setCost(rs.getLong("cost"));
        String rate = rs.getString("rate");
        if (rate != null) {
            invoiceCartPosition.setTaxMode(new TaxMode(rate));
        }
        return invoiceCartPosition;
    };

    private static RowMapper<InvoicingMessage> messageRowMapper = (rs, i) -> {
        InvoicingMessage message = new InvoicingMessage();
        message.setId(rs.getLong(ID));
        message.setEventId(rs.getLong(EVENT_ID));
        message.setEventTime(rs.getString(EVENT_TIME));
        message.setType(rs.getString(TYPE));
        message.setPartyId(rs.getString(PARTY_ID));
        message.setEventType(EventType.valueOf(rs.getString(EVENT_TYPE)));
        Invoice invoice = new Invoice();
        message.setInvoice(invoice);
        invoice.setId(rs.getString(INVOICE_ID));
        invoice.setShopID(rs.getString(SHOP_ID));
        invoice.setCreatedAt(rs.getString(INVOICE_CREATED_AT));
        invoice.setStatus(rs.getString(INVOICE_STATUS));
        invoice.setReason(rs.getString(INVOICE_REASON));
        invoice.setDueDate(rs.getString(INVOICE_DUE_DATE));
        invoice.setAmount(rs.getLong(INVOICE_AMOUNT));
        invoice.setCurrency(rs.getString(INVOICE_CURRENCY));
        InvoiceContent metadata = new InvoiceContent();
        metadata.setType(rs.getString(INVOICE_CONTENT_TYPE));
        metadata.setData(rs.getBytes(INVOICE_CONTENT_DATA));
        invoice.setMetadata(metadata);
        invoice.setProduct(rs.getString(INVOICE_PRODUCT));
        invoice.setDescription(rs.getString(INVOICE_DESCRIPTION));
        if (message.isPayment()) {
            Payment payment = new Payment();
            message.setPayment(payment);
            payment.setId(rs.getString(PAYMENT_ID));
            payment.setCreatedAt(rs.getString(PAYMENT_CREATED_AT));
            payment.setStatus(rs.getString(PAYMENT_STATUS));
            if (rs.getString(PAYMENT_ERROR_CODE) != null && "failed".equals(rs.getString(PAYMENT_STATUS))) {
                payment.setError(new PaymentStatusError(rs.getString(PAYMENT_ERROR_CODE), rs.getString(PAYMENT_ERROR_MESSAGE)));
            }
            payment.setAmount(rs.getLong(PAYMENT_AMOUNT));
            payment.setCurrency(rs.getString(PAYMENT_CURRENCY));
            payment.setPaymentToolToken(rs.getString(PAYMENT_TOOL_TOKEN));
            payment.setPaymentSession(rs.getString(PAYMENT_SESSION));
            payment.setContactInfo(new PaymentContactInfo(rs.getString(PAYMENT_EMAIL), rs.getString(PAYMENT_PHONE)));
            payment.setIp(rs.getString(PAYMENT_IP));
            payment.setFingerprint(rs.getString(PAYMENT_FINGERPRINT));
            Payer.PayerTypeEnum payerType = Payer.PayerTypeEnum.fromValue(rs.getString(PAYMENT_PAYER_TYPE));
            switch (payerType) {
                case CUSTOMERPAYER:
                    payment.setPayer(new CustomerPayer().customerID(rs.getString(PAYMENT_CUSTOMER_ID)));
                    break;
                case PAYMENTRESOURCEPAYER:
                    PaymentResourcePayer payer = new PaymentResourcePayer()
                            .paymentToolToken(rs.getString(PAYMENT_TOOL_TOKEN))
                            .paymentSession(rs.getString(PAYMENT_SESSION))
                            .contactInfo(new ContactInfo()
                                    .email(rs.getString(PAYMENT_EMAIL))
                                    .phoneNumber(rs.getString(PAYMENT_PHONE)))
                            .clientInfo(new ClientInfo()
                                    .fingerprint(rs.getString(PAYMENT_FINGERPRINT))
                                    .ip(rs.getString(PAYMENT_IP)));

                    payer.setPaymentToolDetails(getPaymentToolDetails(rs.getString(PAYMENT_TOOL_DETAILS_TYPE),
                            rs.getString(PAYMENT_CARD_NUMBER_MASK), rs.getString(PAYMENT_SYSTEM), rs.getString(PAYMENT_TERMINAL_PROVIDER),
                            rs.getString(PAYMENT_DIGITAL_WALLET_PROVIDER), rs.getString(PAYMENT_DIGITAL_WALLET_ID)));
                    payment.setPayer(payer);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown payerType "+payerType+"; must be one of these: "+Arrays.toString(Payer.PayerTypeEnum.values()));
            }
            payment.getPayer().setPayerType(payerType);
        }
        return message;
    };

    public InvoicingMessageDaoImpl(DataSource dataSource) {
        setDataSource(dataSource);
    }

    @Override
    public InvoicingMessage getAny(String invoiceId, String type) throws DaoException {
        InvoicingMessage result = null;
        final String sql = "SELECT * FROM hook.message WHERE invoice_id =:invoice_id AND type =:type ORDER BY id DESC LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource(INVOICE_ID, invoiceId).addValue(TYPE, type);
        try {
            result = getNamedParameterJdbcTemplate().queryForObject(sql, params, messageRowMapper);
            final String sqlCarts = "SELECT * FROM hook.cart_position WHERE message_id =:message_id";
            MapSqlParameterSource paramsCarts = new MapSqlParameterSource("message_id", result.getId());
            List<InvoiceCartPosition> cart = getNamedParameterJdbcTemplate().query(sqlCarts, paramsCarts, cartPositionRowMapper);
            if (!cart.isEmpty()) {
                result.getInvoice().setCart(cart);
            }
        } catch (EmptyResultDataAccessException e) {
            log.warn("InvoicingMessage with invoiceId {} not exist!", invoiceId);
        } catch (NestedRuntimeException e) {
            throw new DaoException("InvoicingMessageDaoImpl.getAny error with invoiceId " + invoiceId, e);
        }
        return result;
    }

    private void saveCart(Long messageId, Collection<InvoiceCartPosition> cart) {
        if (cart == null || cart.isEmpty()) return;
        int size = cart.size();
        List<Map<String, Object>> batchValues = new ArrayList<>(size);
        for (InvoiceCartPosition cartPosition : cart) {
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                    .addValue("message_id", messageId)
                    .addValue("product", cartPosition.getProduct())
                    .addValue("price", cartPosition.getPrice())
                    .addValue("quantity", cartPosition.getQuantity())
                    .addValue("cost", cartPosition.getCost())
                    .addValue("rate", cartPosition.getTaxMode() == null ? null : cartPosition.getTaxMode().getRate());
            batchValues.add(mapSqlParameterSource.getValues());
        }

        final String sql = "INSERT INTO hook.cart_position(message_id, product, price, quantity, cost, rate) VALUES (:message_id, :product, :price, :quantity, :cost, :rate) ";

        try {
            int updateCount[] = getNamedParameterJdbcTemplate().batchUpdate(sql, batchValues.toArray(new Map[size]));
            if (updateCount.length != size) {
                throw new DaoException("Couldn't insert cart for messageId " + messageId);
            }
        } catch (NestedRuntimeException e) {
            throw new DaoException("Fail to save cart for messageId " + messageId, e);
        }
    }


    @Override
    @Transactional
    public void create(InvoicingMessage message) throws DaoException {
        final String sql = "INSERT INTO hook.message" +
                "(event_id, event_time, type, party_id, event_type, " +
                "invoice_id, shop_id, invoice_created_at, invoice_status, invoice_reason, invoice_due_date, invoice_amount, " +
                "invoice_currency, invoice_content_type, invoice_content_data, invoice_product, invoice_description, " +
                "payment_id, payment_created_at, payment_status, payment_error_code, payment_error_message, payment_amount, " +
                "payment_currency, payment_tool_token, payment_session, payment_email, payment_phone, payment_ip, payment_fingerprint, " +
                "payment_customer_id, payment_payer_type, payment_tool_details_type, payment_card_number_mask, payment_system, payment_terminal_provider, " +
                "payment_digital_wallet_provider, payment_digital_wallet_id) " +
                "VALUES " +
                "(:event_id, :event_time, :type, :party_id, CAST(:event_type as hook.eventtype), " +
                ":invoice_id, :shop_id, :invoice_created_at, :invoice_status, :invoice_reason, :invoice_due_date, :invoice_amount, " +
                ":invoice_currency, :invoice_content_type, :invoice_content_data, :invoice_product, :invoice_description, " +
                ":payment_id, :payment_created_at, :payment_status, :payment_error_code, :payment_error_message, :payment_amount, " +
                ":payment_currency, :payment_tool_token, :payment_session, :payment_email, :payment_phone, :payment_ip, :payment_fingerprint, " +
                ":payment_customer_id, CAST(:payment_payer_type as hook.payment_payer_type), CAST(:payment_tool_details_type as hook.payment_tool_details_type), " +
                ":payment_card_number_mask, :payment_system, :payment_terminal_provider, :payment_digital_wallet_provider, :payment_digital_wallet_id) " +
                "RETURNING id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(EVENT_ID, message.getEventId())
                .addValue(EVENT_TIME, message.getEventTime())
                .addValue(TYPE, message.getType())
                .addValue(PARTY_ID, message.getPartyId())
                .addValue(EVENT_TYPE, message.getEventType().toString())
                .addValue(INVOICE_ID, message.getInvoice().getId())
                .addValue(SHOP_ID, message.getInvoice().getShopID())
                .addValue(INVOICE_CREATED_AT, message.getInvoice().getCreatedAt())
                .addValue(INVOICE_STATUS, message.getInvoice().getStatus())
                .addValue(INVOICE_REASON, message.getInvoice().getReason())
                .addValue(INVOICE_DUE_DATE, message.getInvoice().getDueDate())
                .addValue(INVOICE_AMOUNT, message.getInvoice().getAmount())
                .addValue(INVOICE_CURRENCY, message.getInvoice().getCurrency())
                .addValue(INVOICE_CONTENT_TYPE, message.getInvoice().getMetadata().getType())
                .addValue(INVOICE_CONTENT_DATA, message.getInvoice().getMetadata().getData())
                .addValue(INVOICE_PRODUCT, message.getInvoice().getProduct())
                .addValue(INVOICE_DESCRIPTION, message.getInvoice().getDescription());
        //TODO
        setNullPaymentParamValues(params);
        if (message.isPayment()) {
            Payment payment = message.getPayment();
            params.addValue(PAYMENT_ID, payment.getId())
                    .addValue(PAYMENT_CREATED_AT,  payment.getCreatedAt())
                    .addValue(PAYMENT_STATUS, payment.getStatus())
                    .addValue(PAYMENT_ERROR_CODE, payment.getError() != null ? payment.getError().getCode() : null)
                    .addValue(PAYMENT_ERROR_MESSAGE, payment.getError() != null ? payment.getError().getMessage() : null)
                    .addValue(PAYMENT_AMOUNT, payment.getAmount())
                    .addValue(PAYMENT_CURRENCY, payment.getCurrency())
                    .addValue(PAYMENT_TOOL_TOKEN, payment.getPaymentToolToken())
                    .addValue(PAYMENT_SESSION, payment.getPaymentSession())
                    .addValue(PAYMENT_EMAIL, payment.getContactInfo().getEmail())
                    .addValue(PAYMENT_PHONE, payment.getContactInfo().getPhoneNumber())
                    .addValue(PAYMENT_IP, payment.getIp())
                    .addValue(PAYMENT_FINGERPRINT, payment.getFingerprint());

            Payer.PayerTypeEnum payerType = payment.getPayer().getPayerType();
            params.addValue(PAYMENT_PAYER_TYPE, payerType.getValue());
            switch (payerType) {
                case CUSTOMERPAYER:
                    params.addValue(PAYMENT_CUSTOMER_ID, ((CustomerPayer)payment.getPayer()).getCustomerID());
                    break;
                case PAYMENTRESOURCEPAYER:
                    PaymentResourcePayer payer = (PaymentResourcePayer) payment.getPayer();
                    params.addValue(PAYMENT_TOOL_TOKEN, payer.getPaymentToolToken())
                            .addValue(PAYMENT_SESSION, payer.getPaymentSession())
                            .addValue(PAYMENT_EMAIL, payer.getContactInfo().getEmail())
                            .addValue(PAYMENT_PHONE,payer.getContactInfo().getPhoneNumber())
                            .addValue(PAYMENT_IP, payer.getClientInfo().getIp())
                            .addValue(PAYMENT_FINGERPRINT, payer.getClientInfo().getFingerprint());

                    PaymentToolUtils.setPaymentToolDetailsParam(params, payer.getPaymentToolDetails(),
                            PAYMENT_TOOL_DETAILS_TYPE, PAYMENT_CARD_NUMBER_MASK, PAYMENT_SYSTEM, PAYMENT_TERMINAL_PROVIDER,
                            PAYMENT_DIGITAL_WALLET_PROVIDER, PAYMENT_DIGITAL_WALLET_ID);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown payerType "+payerType+"; must be one of these: "+Arrays.toString(Payer.PayerTypeEnum.values()));
            }
        }
        try {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            getNamedParameterJdbcTemplate().update(sql, params, keyHolder);
            message.setId(keyHolder.getKey().longValue());
            saveCart(message.getId(), message.getInvoice().getCart());
            log.info("InvoicingMessage {} saved to db.", message);
            queueDao.createWithPolicy(message.getId());
            taskDao.create(message.getId());
        } catch (NestedRuntimeException e) {
            throw new DaoException("Couldn't create message with invoice_id "+ message.getInvoice().getId(), e);
        }
    }

    @Override
    public Long getMaxEventId() {
        final String sql = "SELECT max(event_id) FROM hook.message";
        try {
            return getNamedParameterJdbcTemplate().queryForObject(sql, new HashMap<>(), Long.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<InvoicingMessage> getBy(Collection<Long> messageIds) throws DaoException {
        final String sql = "SELECT * FROM hook.message WHERE id in (:ids)";
        try {
            List<InvoicingMessage> messagesFromDb = getNamedParameterJdbcTemplate().query(sql, new MapSqlParameterSource("ids", messageIds), messageRowMapper);
            log.debug("messagesFromDb {}", messagesFromDb);
            return messagesFromDb;
        }  catch (NestedRuntimeException e) {
            throw new DaoException("InvoicingMessageDaoImpl.getByIds error", e);
        }
    }


}