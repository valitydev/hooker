package dev.vality.hooker.dao;

import dev.vality.damsel.webhooker.EventFilter;
import dev.vality.damsel.webhooker.WebhookParams;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.Hook;
import dev.vality.hooker.model.PartyMetadata;
import dev.vality.hooker.utils.EventFilterUtils;
import dev.vality.hooker.utils.HookConverter;
import dev.vality.swag_webhook_events.model.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by inalarsanukaev on 08.04.17.
 */
@PostgresqlSpringBootITest
public class HookDaoImplTest {

    private List<Long> ids = new ArrayList<>();
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private HookDao hookDao;

    public static Hook buildHook(String partyId, String url) {
        Hook hook = new Hook();
        hook.setPartyId(partyId);
        hook.setUrl(url);
        hook.setTopic(Event.TopicEnum.INVOICESTOPIC.getValue());

        Set<WebhookAdditionalFilter> webhookAdditionalFilters = new HashSet<>();
        webhookAdditionalFilters
                .add(new WebhookAdditionalFilter(EventType.INVOICE_PAYMENT_STATUS_CHANGED, "34", null, "cancelled",
                        "failed"));
        webhookAdditionalFilters.add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_CREATED).build());
        hook.setFilters(webhookAdditionalFilters);

        return hook;
    }

    public static Hook buildCustomerHook(String partyId, String url) {
        Hook hook = new Hook();
        hook.setPartyId(partyId);
        hook.setUrl(url);
        hook.setTopic(Event.TopicEnum.CUSTOMERSTOPIC.getValue());

        Set<WebhookAdditionalFilter> webhookAdditionalFilters = new HashSet<>();
        webhookAdditionalFilters.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_CREATED).build());
        hook.setFilters(webhookAdditionalFilters);

        return hook;
    }

    @BeforeEach
    public void setUp() throws Exception {
        Set<WebhookAdditionalFilter> webhookAdditionalFilters = new HashSet<>();
        webhookAdditionalFilters
                .add(new WebhookAdditionalFilter(EventType.INVOICE_PAYMENT_STATUS_CHANGED, "34", null, "cancelled",
                        null));
        webhookAdditionalFilters.add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_CREATED).build());
        EventFilter eventFilterByCode = EventFilterUtils.getEventFilter(webhookAdditionalFilters);
        eventFilterByCode.getInvoice().setShopId("1");
        WebhookParams webhookParams = new WebhookParams("123", eventFilterByCode, "https://google.com");
        Hook hook = hookDao.create(HookConverter.convert(webhookParams));
        ids.add(hook.getId());
        webhookAdditionalFilters.clear();
        webhookAdditionalFilters
                .add(new WebhookAdditionalFilter(EventType.INVOICE_STATUS_CHANGED, "78", "unpaid", null, null));
        webhookAdditionalFilters
                .add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_PAYMENT_STARTED).shopId("78")
                        .build());
        webhookParams = new WebhookParams("999", EventFilterUtils.getEventFilter(webhookAdditionalFilters),
                "https://yandex.ru");
        hook = hookDao.create(HookConverter.convert(webhookParams));
        ids.add(hook.getId());
        webhookAdditionalFilters.clear();
        webhookAdditionalFilters
                .add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_STATUS_CHANGED).build());
        webhookParams =
                new WebhookParams("123", EventFilterUtils.getEventFilter(webhookAdditionalFilters), "https://2ch.hk/b");
        hook = hookDao.create(HookConverter.convert(webhookParams));
        ids.add(hook.getId());
        String pubKey1 = hook.getPubKey();
        String pubKey2 = hook.getPubKey();
        assertEquals(pubKey1, pubKey2);
        webhookAdditionalFilters.clear();
        webhookAdditionalFilters.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_CREATED).build());
        webhookAdditionalFilters
                .add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_BINDING_SUCCEEDED).build());
        webhookParams =
                new WebhookParams("123", EventFilterUtils.getEventFilter(webhookAdditionalFilters), "https://2ch.hk/b");
        hook = hookDao.create(HookConverter.convert(webhookParams));
    }

    @AfterEach
    public void tearDown() throws Exception {
        List<Hook> list = hookDao.getPartyHooks("123");
        for (Hook w : list) {
            hookDao.delete(w.getId());
        }
        list = hookDao.getPartyHooks("999");
        for (Hook w : list) {
            hookDao.delete(w.getId());
        }
    }

    @Test
    public void testConstraint() {
        Set<WebhookAdditionalFilter> webhookAdditionalFilters = new HashSet<>();
        webhookAdditionalFilters
                .add(new WebhookAdditionalFilter(EventType.INVOICE_PAYMENT_STATUS_CHANGED, "34", null, "failed",
                        "succeeded"));
        webhookAdditionalFilters
                .add(new WebhookAdditionalFilter(EventType.INVOICE_PAYMENT_STATUS_CHANGED, "34", null, "pending",
                        null));
        WebhookParams webhookParams =
                new WebhookParams("123", EventFilterUtils.getEventFilter(webhookAdditionalFilters), "https://2ch.hk/b");
        Hook hook = hookDao.create(HookConverter.convert(webhookParams));
        ids.add(hook.getId());
    }

    @Test
    public void getPartyWebhooks() throws Exception {
        assertEquals(
                hookDao.getPartyHooks("123").stream().filter(Hook::isEnabled).collect(Collectors.toList()).size(),
                3);
        assertTrue(hookDao.getPartyHooks("88888").stream().filter(Hook::isEnabled).collect(Collectors.toList()).isEmpty());
    }

    @Test
    public void getWebhookById() throws Exception {
        List<Hook> list = hookDao.getPartyHooks("123");
        for (Hook w : list) {
            assertNotNull(hookDao.getHookById(w.getId()));
        }
    }

    @Test
    public void getPartyMetadataTest() {
        assertNull(hookDao.getPartyMetadata("123"));
        jdbcTemplate.update("update hook.party_data set metadata =:metadata where party_id=:party_id",
                new MapSqlParameterSource("party_id", "123")
                        .addValue("metadata", "{\"hooksLimit\":{\"perShop\":{\"1\":22},\"perParty\":15}}"));
        PartyMetadata partyMetadata = hookDao.getPartyMetadata("123");
        assertNotNull(partyMetadata);
        assertEquals(15, partyMetadata.getPartyLimit(10).intValue());
        assertEquals(22, partyMetadata.getShopLimit("1", 10).intValue());
        assertEquals(10, partyMetadata.getShopLimit("kkekeke", 10).intValue());
    }

    @Test
    public void getShopHooksCountTest() {
        assertEquals(1, hookDao.getShopHooksCount("123", "1"));
        assertEquals(0, hookDao.getShopHooksCount("kekekekekeke", "1"));
    }

    @Test
    public void getPartyHooksCountTest() {
        assertEquals(2, hookDao.getPartyHooksCount("123"));
        assertEquals(0, hookDao.getPartyHooksCount("keke"));
    }

    @Test
    public void updateAvailabilityTest() {
        double availability = 0.1;
        hookDao.getPartyHooks("123").forEach(h -> hookDao.updateAvailability(h.getId(), availability));
        hookDao.getPartyHooks("123").forEach(h -> assertEquals(availability, h.getAvailability(), 0.000000000001));
    }
}
