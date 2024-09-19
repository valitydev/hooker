package dev.vality.hooker.dao.impl;

import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.dao.rowmapper.InvoicingRowMapper;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.exception.NotFoundException;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageKey;
import dev.vality.hooker.model.WebhookMessageModel;
import dev.vality.swag_webhook_events.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicingDaoImpl implements InvoicingMessageDao {

    private final RowMapper<InvoicingMessage> invoicingMessageRowMapper;
    private final RowMapper<WebhookMessageModel<InvoicingMessage>> webhookMessageModelRowMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${parent.not.exist.id:-1}")
    private Long parentNotExistId;

    @Override
    public Long save(InvoicingMessage message) {
        try {
            final String sql = "INSERT INTO hook.message" +
                    "(event_time, sequence_id, change_id, type, party_id, event_type, " +
                    "invoice_id, shop_id, invoice_status, payment_id, payment_status, refund_id, refund_status) " +
                    "VALUES " +
                    "(:event_time, :sequence_id, :change_id, :type, :party_id, " +
                    "CAST(:event_type as hook.eventtype), :invoice_id, :shop_id, :invoice_status, :payment_id, " +
                    ":payment_status, :refund_id, :refund_status) " +
                    "ON CONFLICT (invoice_id, sequence_id, change_id) DO NOTHING " +
                    "RETURNING id";

            MapSqlParameterSource sqlParameterSources = new MapSqlParameterSource()
                            .addValue(InvoicingRowMapper.EVENT_TIME, message.getEventTime())
                            .addValue(InvoicingRowMapper.SEQUENCE_ID, message.getSequenceId())
                            .addValue(InvoicingRowMapper.CHANGE_ID, message.getChangeId())
                            .addValue(InvoicingRowMapper.TYPE, message.getType().getValue())
                            .addValue(InvoicingRowMapper.PARTY_ID, message.getPartyId())
                            .addValue(InvoicingRowMapper.EVENT_TYPE, message.getEventType().toString())
                            .addValue(InvoicingRowMapper.INVOICE_ID, message.getSourceId())
                            .addValue(InvoicingRowMapper.SHOP_ID, message.getShopId())
                            .addValue(InvoicingRowMapper.INVOICE_STATUS, message.getInvoiceStatus().getValue())
                            .addValue(InvoicingRowMapper.PAYMENT_ID, message.getPaymentId())
                            .addValue(InvoicingRowMapper.PAYMENT_STATUS,
                                    message.getPaymentStatus() != null ? message.getPaymentStatus().getValue() : null)
                            .addValue(InvoicingRowMapper.REFUND_ID, message.getRefundId())
                            .addValue(InvoicingRowMapper.REFUND_STATUS,
                                    message.getRefundStatus() != null ? message.getRefundStatus().getValue() : null);
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(sql, sqlParameterSources, keyHolder);
            return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        } catch (NestedRuntimeException e) {
            throw new DaoException("Couldn't save batch messages: " + message.getSourceId(), e);
        }
    }

    @Override
    public InvoicingMessage getInvoicingMessage(InvoicingMessageKey key) throws NotFoundException, DaoException {
        final String sql = "SELECT * FROM hook.message WHERE invoice_id =:invoice_id" +
                " AND (payment_id IS NULL OR payment_id=:payment_id)" +
                " AND (refund_id IS NULL OR refund_id=:refund_id)" +
                " AND type =:type ORDER BY id DESC LIMIT 1";
        var params = new MapSqlParameterSource(InvoicingRowMapper.INVOICE_ID, key.getInvoiceId())
                .addValue(InvoicingRowMapper.PAYMENT_ID, key.getPaymentId())
                .addValue(InvoicingRowMapper.REFUND_ID, key.getRefundId())
                .addValue(InvoicingRowMapper.TYPE, key.getType().getValue());
        try {
            return jdbcTemplate.queryForObject(sql, params, invoicingMessageRowMapper);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(String.format("InvoicingMessage not found %s!", key));
        } catch (NestedRuntimeException e) {
            throw new DaoException(String.format("InvoicingMessage error %s", key), e);
        }
    }

    @Override
    public List<WebhookMessageModel<InvoicingMessage>> getWebhookModels(Long messageId) {
        final String sql = "select m.*, w.id as hook_id, w.url, pk.priv_key"  +
                " from hook.message m" +
                " join hook.webhook w on m.party_id = w.party_id " +
                " and w.enabled and w.topic=CAST(:message_type as hook.message_topic)" +
                " join hook.webhook_to_events wte on wte.hook_id = w.id" +
                " join hook.party_data pk on w.party_id=pk.party_id" +
                " where m.id =:id " +
                " and m.event_type = wte.event_type " +
                " and (m.shop_id = wte.invoice_shop_id or wte.invoice_shop_id is null) " +
                " and (m.invoice_status = wte.invoice_status or wte.invoice_status is null) " +
                " and (m.payment_status = wte.invoice_payment_status or wte.invoice_payment_status is null)" +
                " and (m.refund_status = wte.invoice_payment_refund_status " +
                "      or wte.invoice_payment_refund_status is null)";
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id", messageId)
                .addValue("message_type", Event.TopicEnum.INVOICESTOPIC.getValue());
        return jdbcTemplate.query(sql, mapSqlParameterSource, webhookMessageModelRowMapper);
    }

    @Override
    public Long getParentId(Long hookId, String invoiceId, Long messageId) {
        final String sql = "select m.id" +
                " from hook.message m " +
                " join hook.webhook w on w.id=:hook_id" +
                " join hook.webhook_to_events wte on wte.hook_id = w.id" +
                " where m.invoice_id =:invoice_id" +
                " and m.id <:id " +
                " and timestamp m.event_time > wte.created_at" +
                " and m.event_type = wte.event_type " +
                " and (m.shop_id = wte.invoice_shop_id or wte.invoice_shop_id is null) " +
                " and (m.invoice_status = wte.invoice_status or wte.invoice_status is null) " +
                " and (m.payment_status = wte.invoice_payment_status or wte.invoice_payment_status is null)" +
                " and (m.refund_status = wte.invoice_payment_refund_status " +
                "      or wte.invoice_payment_refund_status is null)" +
                " order by id desc limit 1 ";
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                .addValue("hook_id", hookId)
                .addValue("invoice_id", invoiceId)
                .addValue("id", messageId);
        try {
            return jdbcTemplate.queryForObject(sql, mapSqlParameterSource, Long.class);
        } catch (EmptyResultDataAccessException e) {
            return parentNotExistId;
        }
    }
}
