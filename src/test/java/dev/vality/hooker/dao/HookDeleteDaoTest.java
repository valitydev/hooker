package dev.vality.hooker.dao;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.model.*;
import dev.vality.hooker.service.MessageService;
import dev.vality.hooker.utils.BuildUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@PostgresqlSpringBootITest
@TestPropertySource(properties = "message.scheduler.invoicing.threadPoolSize=0")
public class HookDeleteDaoTest {

    @Autowired
    private HookDao hookDao;

    @Autowired
    private CustomerDaoImpl customerDaoImpl;

    @Autowired
    private MessageService<InvoicingMessage> invoicingService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void testDelete() {
        Hook hook1 = hookDao.create(HookDaoImplTest.buildHook("partyId", "fake.url"));
        Hook hook2 = hookDao.create(HookDaoImplTest.buildHook("partyId2", "fake2.url"));
        invoicingService.process(BuildUtils
                .buildMessage(InvoicingMessageEnum.INVOICE.getValue(), "2345", hook1.getPartyId(),
                        EventType.INVOICE_CREATED, InvoiceStatusEnum.FULFILLED, PaymentStatusEnum.CAPTURED));
        assertEquals(1,
                jdbcTemplate.queryForList("select 1 from hook.invoicing_queue", Map.of(), Integer.class).size());

        hookDao.delete(hook2.getId());
        assertNotEquals(0,
                jdbcTemplate.queryForList("select 1 from hook.invoicing_queue", Map.of(), Integer.class).size());
        hookDao.delete(hook1.getId());
        assertTrue(jdbcTemplate.queryForList("select 1 from hook.scheduled_task", Map.of(), Integer.class).isEmpty());
        assertFalse(hookDao.getHookById(hook1.getId()).isEnabled());
        assertFalse(hookDao.getHookById(hook2.getId()).isEnabled());
    }

    @Test
    public void testDeleteCustomerHooks() {
        Hook hook = hookDao.create(HookDaoImplTest.buildCustomerHook("partyId", "fake.url"));
        Long messageId = customerDaoImpl.save(BuildUtils.buildCustomerMessage(1L, hook.getPartyId(),
                EventType.CUSTOMER_CREATED,
                CustomerMessageEnum.CUSTOMER, "124", "4356"));
        assertEquals(1,
                jdbcTemplate.queryForList("select 1 from hook.customer_queue", Map.of(), Integer.class).size());
        hookDao.delete(hook.getId());
        assertTrue(jdbcTemplate.queryForList("select 1 from hook.scheduled_task", Map.of(), Integer.class).isEmpty());
        assertFalse(hookDao.getHookById(hook.getId()).isEnabled());
    }
}
	