package dev.vality.hooker.dao.impl;

import dev.vality.hooker.dao.CustomerDao;
import dev.vality.hooker.dao.rowmapper.CustomerRowMapper;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
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
public class CustomerDaoImpl implements CustomerDao {

    private final RowMapper<CustomerMessage> customerMessageRowMapper;
    private final RowMapper<WebhookMessageModel<CustomerMessage>> webhookMessageModelRowMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${parent.not.exist.id:-1}")
    private Long parentNotExistId;

    @Override
    public CustomerMessage getAny(String customerId, CustomerMessageEnum type) throws DaoException {
        final String sql = "SELECT * FROM hook.customer_message " +
                "WHERE customer_id =:customer_id AND type=CAST(:type as hook.customer_message_type) " +
                "ORDER BY id DESC LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(CustomerRowMapper.CUSTOMER_ID, customerId)
                .addValue(CustomerRowMapper.TYPE, type.getValue());
        try {
            return jdbcTemplate.queryForObject(sql, params, customerMessageRowMapper);
        } catch (EmptyResultDataAccessException e) {
            log.warn("CustomerMessage with customerId {}, type {} not exist!", customerId, type);
            return null;
        } catch (NestedRuntimeException e) {
            throw new DaoException("CustomerMessageDaoImpl.getAny error with customerId " + customerId, e);
        }
    }

    @Override
    public Long save(CustomerMessage message) throws DaoException {
        final String sql = "INSERT INTO hook.customer_message " +
                "(occured_at, sequence_id, change_id, type, " +
                "party_id, event_type, customer_id, customer_shop_id, binding_id) " +
                "VALUES " +
                "(:occured_at, :sequence_id, :change_id, CAST(:type as hook.customer_message_type), " +
                ":party_id, CAST(:event_type as hook.eventtype), :customer_id, :customer_shop_id,  :binding_id) " +
                "ON CONFLICT (customer_id, sequence_id, change_id) DO NOTHING " +
                "RETURNING id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(CustomerRowMapper.OCCURED_AT, message.getEventTime())
                .addValue(CustomerRowMapper.SEQUENCE_ID, message.getSequenceId())
                .addValue(CustomerRowMapper.CHANGE_ID, message.getChangeId())
                .addValue(CustomerRowMapper.TYPE, message.getType().getValue())
                .addValue(CustomerRowMapper.PARTY_ID, message.getPartyId())
                .addValue(CustomerRowMapper.EVENT_TYPE, message.getEventType().name())
                .addValue(CustomerRowMapper.CUSTOMER_ID, message.getSourceId())
                .addValue(CustomerRowMapper.CUSTOMER_SHOP_ID, message.getShopId())
                .addValue(CustomerRowMapper.BINDING_ID, message.getBindingId());
        try {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(sql, params, keyHolder);
            Number holderKey = keyHolder.getKey();
            if (holderKey != null) {
                log.info("CustomerMessage {} saved to db.", message);
                return holderKey.longValue();
            }
            return null;
        } catch (NestedRuntimeException e) {
            throw new DaoException("Couldn't create customerMessage with customerId " + message.getSourceId(), e);
        }
    }

    @Override
    public List<WebhookMessageModel<CustomerMessage>> getWebhookModels(Long messageId) {
        final String sql = "select m.*, w.id as hook_id, w.url, pk.priv_key"  +
                " from hook.customer_message m" +
                " join hook.webhook w on m.party_id = w.party_id " +
                " and w.enabled and w.topic=CAST(:message_type as hook.message_topic)" +
                " join hook.webhook_to_events wte on wte.hook_id = w.id" +
                " join hook.party_data pk on w.party_id=pk.party_id" +
                " where m.id =:id " +
                " and m.event_type = wte.event_type " +
                " and (m.customer_shop_id = wte.invoice_shop_id or wte.invoice_shop_id is null) ";
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                .addValue("id", messageId)
                .addValue("message_type", Event.TopicEnum.CUSTOMERSTOPIC.getValue());
        return jdbcTemplate.query(sql, mapSqlParameterSource, webhookMessageModelRowMapper);
    }

    @Override
    public Long getParentId(Long hookId, String customerId, Long messageId) {
        final String sql = "select m.id"  +
                " from hook.customer_message m " +
                " join hook.webhook w on w.id=:hook_id" +
                " join hook.webhook_to_events wte on wte.hook_id = w.id" +
                " where m.customer_id =:customer_id" +
                " and m.id <:id " +
                " and m.event_type = wte.event_type " +
                " and (m.customer_shop_id = wte.invoice_shop_id or wte.invoice_shop_id is null) " +
                " order by id desc limit 1 ";
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                .addValue("hook_id", hookId)
                .addValue("customer_id", customerId)
                .addValue("id", messageId);
        try {
            return jdbcTemplate.queryForObject(sql, mapSqlParameterSource, Long.class);
        } catch (EmptyResultDataAccessException e) {
            return parentNotExistId;
        }
    }
}