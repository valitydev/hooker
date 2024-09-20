package dev.vality.hooker.dao.impl;

import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.dao.WebhookAdditionalFilter;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.Hook;
import dev.vality.hooker.model.PartyMetadata;
import dev.vality.hooker.model.PartyMetadataRowMapper;
import dev.vality.hooker.service.crypt.KeyPair;
import dev.vality.hooker.service.crypt.Signer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HookDaoImpl implements HookDao {

    private static RowMapper<AllHookTablesRow> allHookTablesRowRowMapper =
            (rs, i) -> new AllHookTablesRow(rs.getLong("id"),
                    rs.getString("party_id"),
                    rs.getString("topic"),
                    rs.getString("url"),
                    rs.getString("pub_key"),
                    rs.getBoolean("enabled"),
                    rs.getDouble("availability"),
                    rs.getString("created_at"),
                    new WebhookAdditionalFilter(EventType.valueOf(rs.getString("event_type")),
                            rs.getString("invoice_shop_id"),
                            rs.getString("invoice_status"),
                            rs.getString("invoice_payment_status"),
                            rs.getString("invoice_payment_refund_status")));
    private final Signer signer;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PartyMetadataRowMapper partyMetadataRowMapper;

    @Override
    public List<Hook> getPartyHooks(String partyId) throws DaoException {
        log.debug("getPartyHooks request. PartyId = {}", partyId);
        final String sql =
                " select w.id, " +
                        " w.party_id, " +
                        " w.url, " +
                        " w.retry_policy, " +
                        " w.enabled, " +
                        " w.topic, " +
                        " w.availability, " +
                        " w.created_at, " +
                        " k.pub_key, " +
                        " wte.hook_id, " +
                        " wte.event_type, " +
                        " wte.invoice_shop_id," +
                        " wte.invoice_status," +
                        " wte.invoice_payment_status," +
                        " wte.invoice_payment_refund_status " +
                        " from hook.webhook w " +
                        " join hook.party_data k " +
                        " on w.party_id = k.party_id " +
                        " join hook.webhook_to_events wte " +
                        " on wte.hook_id = w.id " +
                        " where w.party_id =:party_id " +
                        " order by w.id";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("party_id", partyId);

        try {
            List<AllHookTablesRow> allHookTablesRows = jdbcTemplate.query(sql, params, allHookTablesRowRowMapper);
            List<Hook> result = squashToWebhooks(allHookTablesRows);
            log.debug("getPartyHooks response. Hooks: " + result);
            return result;
        } catch (NestedRuntimeException e) {
            String message = "Couldn't getPartyHooks for partyId " + partyId;
            log.warn(message, e);
            throw new DaoException(message);
        }
    }

    @Override
    public PartyMetadata getPartyMetadata(String partyId) throws DaoException {
        String sql = "select metadata from hook.party_data where party_id=:party_id";
        MapSqlParameterSource params = new MapSqlParameterSource("party_id", partyId);
        try {
            return jdbcTemplate.queryForObject(sql, params, partyMetadataRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (NestedRuntimeException e) {
            String message = "Couldn't getPartyMetadata for partyId " + partyId;
            log.warn(message, e);
            throw new DaoException(message);
        }
    }

    @Override
    public int getShopHooksCount(String partyId, String shopId) throws DaoException {
        String sql = " with we as (" +
                " select wte.hook_id " +
                " from hook.webhook_to_events wte " +
                " join hook.webhook wh on wte.hook_id = wh.id " +
                " where wh.party_id =:party_id " +
                "   and wte.invoice_shop_id =:shop_id " +
                "   and wh.enabled " +
                " group by hook_id) " +
                " select count(1) as cnt from we";
        MapSqlParameterSource params = new MapSqlParameterSource("party_id", partyId)
                .addValue("shop_id", shopId);
        try {
            return jdbcTemplate.queryForObject(sql, params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        } catch (NestedRuntimeException e) {
            String message = String.format("Couldn't getHooksCount for partyId=%s, shopId=%s", partyId, shopId);
            log.warn(message, e);
            throw new DaoException(message);
        }
    }

    @Override
    public int getPartyHooksCount(String partyId) throws DaoException {
        String sql = " with we as (" +
                " select wte.hook_id " +
                " from hook.webhook_to_events wte " +
                " join hook.webhook wh on wte.hook_id = wh.id " +
                " where wh.party_id =:party_id " +
                "   and wte.invoice_shop_id is null " +
                "   and wh.enabled " +
                " group by hook_id) " +
                " select count(1) as cnt from we ";
        MapSqlParameterSource params = new MapSqlParameterSource("party_id", partyId);
        try {
            return jdbcTemplate.queryForObject(sql, params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        } catch (NestedRuntimeException e) {
            String message = String.format("Couldn't getPartyHooksCount for partyId=%s", partyId);
            log.warn(message, e);
            throw new DaoException(message);
        }
    }

    private List<Hook> squashToWebhooks(List<AllHookTablesRow> allHookTablesRows) {
        List<Hook> result = new ArrayList<>();
        if (allHookTablesRows == null || allHookTablesRows.isEmpty()) {
            return result;
        }
        final Map<Long, List<AllHookTablesRow>> hookIdToRows = new HashMap<>();

        //grouping by hookId
        for (AllHookTablesRow row : allHookTablesRows) {
            final long hookId = row.getId();
            List<AllHookTablesRow> list = hookIdToRows.computeIfAbsent(hookId, k -> new ArrayList<>());
            list.add(row);
        }

        for (long hookId : hookIdToRows.keySet()) {
            List<AllHookTablesRow> rows = hookIdToRows.get(hookId);
            Hook hook = new Hook();
            hook.setId(hookId);
            AllHookTablesRow allHookTablesRow = rows.get(0);
            hook.setPartyId(allHookTablesRow.getPartyId());
            hook.setTopic(allHookTablesRow.getTopic());
            hook.setUrl(allHookTablesRow.getUrl());
            hook.setPubKey(allHookTablesRow.getPubKey());
            hook.setEnabled(allHookTablesRow.isEnabled());
            hook.setAvailability(allHookTablesRow.getAvailability());
            hook.setCreatedAt(allHookTablesRow.getCreatedAt());
            hook.setFilters(rows.stream()
                    .map(AllHookTablesRow::getWebhookAdditionalFilter)
                    .collect(Collectors.toSet()));
            result.add(hook);
        }

        return result;
    }

    @Override
    public Hook getHookById(long id) throws DaoException {
        final String sql = "select w.id, " +
                " w.party_id, " +
                " w.url, " +
                " w.retry_policy, " +
                " w.enabled, " +
                " w.topic, " +
                " w.availability, " +
                " w.created_at, " +
                " k.pub_key, " +
                " wte.hook_id, " +
                " wte.event_type, " +
                " wte.invoice_shop_id, " +
                " wte.invoice_status, " +
                " wte.invoice_payment_status, " +
                " wte.invoice_payment_refund_status " +
                " from hook.webhook w " +
                " join hook.party_data k " +
                " on w.party_id = k.party_id " +
                " join hook.webhook_to_events wte " +
                " on wte.hook_id = w.id " +
                " where w.id =:id";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        try {
            List<AllHookTablesRow> allHookTablesRows = jdbcTemplate.query(sql, params, allHookTablesRowRowMapper);
            List<Hook> result = squashToWebhooks(allHookTablesRows);
            if (result == null || result.isEmpty()) {
                return null;
            }
            return result.get(0);
        } catch (NestedRuntimeException e) {
            log.warn("Fail to get hook {}", id, e);
            throw new DaoException(e);
        }
    }

    @Override
    @Transactional
    public Hook create(Hook hook) throws DaoException {
        String pubKey = createOrGetPubKey(hook.getPartyId());
        hook.setPubKey(pubKey);
        hook.setEnabled(true);
        final String sql = "INSERT INTO hook.webhook(party_id, url, topic, created_at) " +
                "VALUES (:party_id, :url, CAST(:topic as hook.message_topic), null) RETURNING ID";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("party_id", hook.getPartyId())
                .addValue("url", hook.getUrl())
                .addValue("topic", hook.getTopic());
        try {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            int updateCount = jdbcTemplate.update(sql, params, keyHolder);
            if (updateCount != 1) {
                throw new DaoException("Couldn't insert webhook " + hook.getId() + " into table");
            }
            hook.setId(keyHolder.getKey().longValue());
            saveHookFilters(hook.getId(), hook.getFilters());
        } catch (NestedRuntimeException e) {
            log.warn("Fail to createWithPolicy hook {}", hook, e);
            throw new DaoException(e);
        }
        log.info("Webhook with id = {} created.", hook.getId());
        return hook;
    }

    private void saveHookFilters(long hookId, Collection<WebhookAdditionalFilter> webhookAdditionalFilters) {
        int size = webhookAdditionalFilters.size();
        List<Map<String, Object>> batchValues = new ArrayList<>(size);
        for (WebhookAdditionalFilter webhookAdditionalFilter : webhookAdditionalFilters) {
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource("hook_id", hookId)
                    .addValue("event_type", webhookAdditionalFilter.getEventType().toString())
                    .addValue("invoice_shop_id", webhookAdditionalFilter.getShopId())
                    .addValue("invoice_status", webhookAdditionalFilter.getInvoiceStatus())
                    .addValue("invoice_payment_status", webhookAdditionalFilter.getInvoicePaymentStatus())
                    .addValue("invoice_payment_refund_status", webhookAdditionalFilter.getInvoicePaymentRefundStatus());
            batchValues.add(mapSqlParameterSource.getValues());
        }

        final String sql = "INSERT INTO hook.webhook_to_events(hook_id, event_type, invoice_shop_id, invoice_status, " +
                "invoice_payment_status, invoice_payment_refund_status) VALUES " +
                "(:hook_id, CAST(:event_type AS hook.eventtype), :invoice_shop_id, :invoice_status, " +
                ":invoice_payment_status, :invoice_payment_refund_status)";

        try {
            int[] updateCount = jdbcTemplate.batchUpdate(sql, batchValues.toArray(new Map[size]));
            if (updateCount.length != size) {
                throw new DaoException("Couldn't insert relation between hook and events.");
            }
        } catch (NestedRuntimeException e) {
            log.error("Fail to save hook filters.", e);
            throw new DaoException(e);
        }
    }

    @Override
    public void delete(long id) throws DaoException {
        final String sql = " UPDATE hook.webhook SET enabled = FALSE where id=:id;";
        try {
            jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void updateAvailability(long id, double availability) throws DaoException {
        final String sql =
                "update hook.webhook set availability =:availability where id =:id ";
        try {
            jdbcTemplate.update(sql, new MapSqlParameterSource("id", id)
                    .addValue("availability", availability));
        } catch (NestedRuntimeException e) {
            throw new DaoException(e);
        }
    }

    private String createOrGetPubKey(String partyId) throws DaoException {
        final String sql = "INSERT INTO hook.party_data(party_id, priv_key, pub_key) " +
                "VALUES (:party_id, :priv_key, :pub_key) " +
                "ON CONFLICT(party_id) DO UPDATE SET party_id=:party_id RETURNING pub_key";

        KeyPair keyPair = signer.generateKeys();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("party_id", partyId)
                .addValue("priv_key", keyPair.getPrivKey())
                .addValue("pub_key", keyPair.getPublKey());
        String pubKey = null;
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(sql, params, keyHolder);
            pubKey = (String) keyHolder.getKeys().get("pub_key");
        } catch (NestedRuntimeException | NullPointerException | ClassCastException e) {
            log.warn("Fail to createOrGetPubKey security keys for party {} ", partyId, e);
            throw new DaoException(e);
        }
        return pubKey;
    }
}
