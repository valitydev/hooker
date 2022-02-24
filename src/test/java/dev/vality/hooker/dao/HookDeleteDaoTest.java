package dev.vality.hooker.dao;

import dev.vality.hooker.AbstractIntegrationTest;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.dao.impl.CustomerQueueDao;
import dev.vality.hooker.dao.impl.CustomerTaskDao;
import dev.vality.hooker.dao.impl.InvoicingQueueDao;
import dev.vality.hooker.dao.impl.InvoicingTaskDao;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.Hook;
import dev.vality.hooker.model.InvoiceStatusEnum;
import dev.vality.hooker.model.InvoicingMessageEnum;
import dev.vality.hooker.model.PaymentStatusEnum;
import dev.vality.hooker.service.BatchService;
import dev.vality.hooker.utils.BuildUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static dev.vality.hooker.utils.BuildUtils.buildCustomerMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "message.scheduler.invoicing.threadPoolSize=0")
public class HookDeleteDaoTest extends AbstractIntegrationTest {

    @Autowired
    InvoicingTaskDao taskDao;

    @Autowired
    CustomerTaskDao customerTaskDao;

    @Autowired
    InvoicingQueueDao queueDao;

    @Autowired
    CustomerQueueDao customerQueueDao;

    @Autowired
    HookDao hookDao;

    @Autowired
    CustomerDaoImpl customerDaoImpl;

    @Autowired
    BatchService batchService;

    @Test
    public void testDelete() {
        Hook hook1 = hookDao.create(HookDaoImplTest.buildHook("partyId", "fake.url"));
        Hook hook2 = hookDao.create(HookDaoImplTest.buildHook("partyId2", "fake2.url"));
        batchService.process(Collections.singletonList(BuildUtils
                .buildMessage(InvoicingMessageEnum.INVOICE.getValue(), "2345", hook1.getPartyId(),
                        EventType.INVOICE_CREATED, InvoiceStatusEnum.FULFILLED, PaymentStatusEnum.CAPTURED)));
        assertEquals(queueDao.getWithPolicies(taskDao.getScheduled().keySet()).size(), 1);

        hookDao.delete(hook2.getId());
        assertNotEquals(queueDao.getWithPolicies(taskDao.getScheduled().keySet()).size(), 0);
        hookDao.delete(hook1.getId());
        assertTrue(taskDao.getScheduled().keySet().isEmpty());
        assertFalse(hookDao.getHookById(hook1.getId()).isEnabled());
        assertFalse(hookDao.getHookById(hook2.getId()).isEnabled());
    }

    @Test
    public void testDeleteCustomerHooks() {
        Hook hook = hookDao.create(HookDaoImplTest.buildCustomerHook("partyId", "fake.url"));
        Long messageId = customerDaoImpl.create(buildCustomerMessage(1L, hook.getPartyId(), EventType.CUSTOMER_CREATED,
                CustomerMessageEnum.CUSTOMER, "124", "4356"));
        customerQueueDao.createWithPolicy(messageId);
        customerTaskDao.create(messageId);
        assertEquals(customerQueueDao.getWithPolicies(customerTaskDao.getScheduled().keySet()).size(), 1);
        hookDao.delete(hook.getId());
        assertTrue(customerTaskDao.getScheduled().keySet().isEmpty());
        assertFalse(hookDao.getHookById(hook.getId()).isEnabled());
    }
}
