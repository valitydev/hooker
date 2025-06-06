package dev.vality.hooker.service;

import dev.vality.damsel.webhooker.*;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.utils.HookConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PostgresqlSpringBootITest
@SpringBootTest
class HooksLimitServiceTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private HooksLimitService hooksLimitService;

    @Autowired
    private HookDao hookDao;

    @Test
    void isLimitExceededFalseTest() {
        WebhookParams webhookParams = buildWebhookParams();

        hookDao.create(HookConverter.convert(webhookParams));
        assertFalse(hooksLimitService.isLimitExceeded(webhookParams));
    }

    @Test
    void isShopLimitExceededTrueTest() {
        WebhookParams webhookParams = buildWebhookParams();

        IntStream.range(1, 11).forEach(x -> hookDao.create(HookConverter.convert(webhookParams)));
        assertTrue(hooksLimitService.isLimitExceeded(webhookParams));
    }

    @Test
    void isShopLimitExceededFalseTest() {
        WebhookParams webhookParams = buildWebhookParams();

        IntStream.range(1, 11).forEach(x -> hookDao.create(HookConverter.convert(webhookParams)));
        jdbcTemplate.update("update hook.party_data set metadata =:metadata where party_id=:party_id",
                new MapSqlParameterSource("party_id", "party_id")
                        .addValue("metadata", "{\"hooksLimit\":{\"perShop\":{\"shop_id\":22},\"perParty\":15}}"));
        assertFalse(hooksLimitService.isLimitExceeded(webhookParams));
    }

    @Test
    void isPartyLimitExceededTrueTest() {
        WebhookParams webhookParams = buildWebhookParams();
        webhookParams.getEventFilter().getInvoice().setShopId(null);

        IntStream.range(1, 6).forEach(x -> hookDao.create(HookConverter.convert(webhookParams)));
        assertTrue(hooksLimitService.isLimitExceeded(webhookParams));
    }


    @AfterEach
    void after() {
        jdbcTemplate.update("truncate hook.webhook cascade", new MapSqlParameterSource());
        jdbcTemplate.update("truncate hook.party_data cascade", new MapSqlParameterSource());
    }

    private WebhookParams buildWebhookParams() {
        return new WebhookParams()
                .setPartyId("party_id")
                .setUrl("http://2ch.hk")
                .setEventFilter(EventFilter.invoice(
                        new InvoiceEventFilter()
                                .setShopId("shop_id")
                                .setTypes(Set.of(InvoiceEventType.created(new InvoiceCreated())))));
    }

}
