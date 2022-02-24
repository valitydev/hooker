package dev.vality.hooker.service;

import dev.vality.damsel.base.Content;
import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.ClientInfo;
import dev.vality.damsel.domain.ContactInfo;
import dev.vality.damsel.domain.CurrencyRef;
import dev.vality.damsel.domain.DisposablePaymentResource;
import dev.vality.damsel.domain.Invoice;
import dev.vality.damsel.domain.InvoiceCart;
import dev.vality.damsel.domain.InvoiceDetails;
import dev.vality.damsel.domain.InvoiceLine;
import dev.vality.damsel.domain.InvoicePayment;
import dev.vality.damsel.domain.InvoicePaymentFlow;
import dev.vality.damsel.domain.InvoicePaymentPending;
import dev.vality.damsel.domain.InvoicePaymentProcessed;
import dev.vality.damsel.domain.InvoicePaymentStatus;
import dev.vality.damsel.domain.InvoiceStatus;
import dev.vality.damsel.domain.InvoiceUnpaid;
import dev.vality.damsel.domain.LegacyBankCardPaymentSystem;
import dev.vality.damsel.domain.Payer;
import dev.vality.damsel.domain.PaymentResourcePayer;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoiceCreated;
import dev.vality.damsel.payment_processing.InvoicePaymentChange;
import dev.vality.damsel.payment_processing.InvoicePaymentChangePayload;
import dev.vality.damsel.payment_processing.InvoicePaymentStarted;
import dev.vality.damsel.payment_processing.InvoicePaymentStatusChanged;
import dev.vality.hooker.AbstractIntegrationTest;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.dao.impl.InvoicingMessageDaoImpl;
import dev.vality.hooker.dao.impl.InvoicingQueueDao;
import dev.vality.hooker.dao.impl.InvoicingTaskDao;
import dev.vality.hooker.handler.poller.invoicing.AbstractInvoiceEventMapper;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageEnum;
import dev.vality.hooker.model.InvoicingMessageKey;
import dev.vality.hooker.model.PaymentStatusEnum;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.hooker.utils.KeyUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BatchProcessingTest extends AbstractIntegrationTest {

    @Autowired
    private HandlerManager handlerManager;

    @Autowired
    private BatchService batchService;

    @Autowired
    private InvoicingMessageDaoImpl messageDao;

    @Autowired
    private InvoicingTaskDao taskDao;

    @Autowired
    private InvoicingQueueDao invoicingQueueDao;

    @Autowired
    private HookDao hookDao;

    @Test
    public void testBatchProcess() {
        hookDao.create(BuildUtils.buildHook("partyId", "www.kek.ru", EventType.INVOICE_CREATED));

        LinkedHashMap<InvoicingMessageKey, InvoicingMessage> storage = new LinkedHashMap<>();

        InvoiceChange ic = getInvoiceCreated();
        EventInfo eventInfo = new EventInfo(null, "2016-03-22T06:12:27Z", "invoiceId", 1L, 1);
        Optional<AbstractInvoiceEventMapper> eventMapperOptional = handlerManager.getHandler(ic);
        assertTrue(eventMapperOptional.isPresent());
        AbstractInvoiceEventMapper invoiceEventMapper = eventMapperOptional.get();
        InvoicingMessage invoiceCreated = invoiceEventMapper.handle(ic, eventInfo, storage);
        assertNotNull(invoiceCreated);
        assertEquals("invoiceId", invoiceCreated.getInvoiceId());
        assertEquals(InvoicingMessageEnum.INVOICE, invoiceCreated.getType());
        storage.put(KeyUtils.key(invoiceCreated), invoiceCreated);

        InvoiceChange icPaymentStarted = getInvoicePaymentStarted();
        EventInfo eventInfoPaymentStarted = new EventInfo(null, "2016-03-22T06:12:27Z", "invoiceId", 1L, 2);
        Optional<AbstractInvoiceEventMapper> eventMapperPaymentStartedOptional =
                handlerManager.getHandler(icPaymentStarted);
        assertTrue(eventMapperPaymentStartedOptional.isPresent());
        AbstractInvoiceEventMapper invoicePaymentStartedEventMapper = eventMapperPaymentStartedOptional.get();
        InvoicingMessage paymentStarted =
                invoicePaymentStartedEventMapper.handle(icPaymentStarted, eventInfoPaymentStarted, storage);
        assertNotNull(paymentStarted);
        assertEquals(InvoicingMessageEnum.PAYMENT, paymentStarted.getType());
        assertEquals("partyId", paymentStarted.getPartyId());
        assertNotEquals(invoiceCreated.getChangeId(), paymentStarted.getChangeId());

        List<InvoicingMessage> messages = new ArrayList<>();

        storage.put(KeyUtils.key(invoiceCreated), invoiceCreated);
        storage.put(KeyUtils.key(paymentStarted), paymentStarted);
        messages.add(invoiceCreated);
        messages.add(paymentStarted);

        assertEquals(2, storage.size());
        assertEquals(2, messages.size());

        InvoiceChange icStatusChanged = getInvoicePaymentChangeStatus();
        EventInfo eventInfoPaymentStatusChanged = new EventInfo(null, "2016-03-22T06:12:27Z", "invoiceId", 1L, 3);
        Optional<AbstractInvoiceEventMapper> eventMapperPaymentStatusChangedOptional =
                handlerManager.getHandler(icStatusChanged);
        assertTrue(eventMapperPaymentStatusChangedOptional.isPresent());
        AbstractInvoiceEventMapper invoicePaymentStatusChangedEventMapper =
                eventMapperPaymentStatusChangedOptional.get();
        InvoicingMessage statusChanged =
                invoicePaymentStatusChangedEventMapper.handle(icStatusChanged, eventInfoPaymentStatusChanged, storage);
        assertNotNull(statusChanged);
        assertEquals("partyId", statusChanged.getPartyId());
        assertEquals(PaymentStatusEnum.PROCESSED, statusChanged.getPaymentStatus());
        assertNotEquals(statusChanged.getPaymentStatus(), paymentStarted.getPaymentStatus());
        storage.put(KeyUtils.key(statusChanged), statusChanged);
        messages.add(statusChanged);

        assertEquals(2, storage.size());
        assertEquals(3, messages.size());

        // not found message
        InvoiceChange icStatusNotFoundChanged = getInvoicePaymentChangeStatus();
        EventInfo eventInfoPaymentStatusNotFoundChanged =
                new EventInfo(null, "2016-03-22T06:12:27Z", "not_found", 1L, 3);
        Optional<AbstractInvoiceEventMapper> eventMapperPaymentStatusNotFoundChangedOptional =
                handlerManager.getHandler(icStatusNotFoundChanged);
        assertTrue(eventMapperPaymentStatusChangedOptional.isPresent());
        AbstractInvoiceEventMapper invoicePaymentStatusNotFoundChangedEventMapper =
                eventMapperPaymentStatusNotFoundChangedOptional.get();
        InvoicingMessage statusNotFoundChanged = invoicePaymentStatusNotFoundChangedEventMapper
                .handle(icStatusNotFoundChanged, eventInfoPaymentStatusNotFoundChanged, storage);
        assertNull(statusNotFoundChanged);

        batchService.process(messages);
        //
        InvoicingMessage invoiceCreatedFromDB = messageDao.getInvoicingMessage(KeyUtils.key(invoiceCreated));
        assertNotNull(invoiceCreatedFromDB);
        assertNotNull(invoiceCreatedFromDB.getId());
        assertNotNull(invoiceCreatedFromDB.getEventId());
        InvoicingMessage lastStateOfPayment = messageDao.getInvoicingMessage(KeyUtils.key(statusChanged));
        assertNotNull(lastStateOfPayment);
        assertNotEquals(lastStateOfPayment.getId(), invoiceCreatedFromDB.getId());
        InvoicingMessage theSameLastStateOfPayment = messageDao.getInvoicingMessage(KeyUtils.key(paymentStarted));
        assertNotNull(theSameLastStateOfPayment);
        Long messageId = theSameLastStateOfPayment.getId();
        assertEquals(messageId, lastStateOfPayment.getId());

        assertNotEquals(messageDao.getBy(Collections.singletonList(messageId - 1)).get(0).getPaymentStatus(),
                messageDao.getBy(Collections.singletonList(messageId)).get(0).getPaymentStatus());

        assertEquals(1, taskDao.getScheduled().size());
        assertEquals(1, invoicingQueueDao.getWithPolicies(Collections.singletonList(1L)).size());

        //test duplication
        batchService.process(messages);
        assertEquals(1, invoicingQueueDao.getWithPolicies(Collections.singletonList(1L)).size());
    }

    private InvoiceChange getInvoicePaymentStarted() {
        InvoicePayment payment = new InvoicePayment()
                .setId("1")
                .setCreatedAt("2016-03-22T06:12:27Z")
                .setStatus(InvoicePaymentStatus.pending(new InvoicePaymentPending()))
                .setPayer(Payer.payment_resource(
                        new PaymentResourcePayer()
                                .setResource(new DisposablePaymentResource()
                                        .setPaymentTool(PaymentTool.bank_card(new BankCard()
                                                .setToken("token")
                                                .setPaymentSystemDeprecated(LegacyBankCardPaymentSystem.amex)
                                                .setBin("bin")
                                                .setLastDigits("masked")))
                                        .setClientInfo(new ClientInfo()))
                                .setContactInfo(new ContactInfo())))
                .setCost(new Cash()
                        .setAmount(123L)
                        .setCurrency(new CurrencyRef()
                                .setSymbolicCode("RUB")))
                .setFlow(new InvoicePaymentFlow());

        return InvoiceChange.invoice_payment_change(
                new InvoicePaymentChange()
                        .setId("1")
                        .setPayload(
                                InvoicePaymentChangePayload.invoice_payment_started(
                                        new InvoicePaymentStarted().setPayment(payment)
                                )));
    }


    private InvoiceChange getInvoicePaymentChangeStatus() {
        return InvoiceChange.invoice_payment_change(
                new InvoicePaymentChange()
                        .setId("1")
                        .setPayload(InvoicePaymentChangePayload.invoice_payment_status_changed(
                                new InvoicePaymentStatusChanged()
                                        .setStatus(InvoicePaymentStatus.processed(new InvoicePaymentProcessed())))));
    }

    @NotNull
    private InvoiceChange getInvoiceCreated() {
        InvoiceChange ic = new InvoiceChange();
        InvoiceCreated invoiceCreated = new InvoiceCreated();
        Invoice invoice = new Invoice();
        invoiceCreated.setInvoice(
                invoice.setId("invoiceId")
                        .setOwnerId("partyId")
                        .setShopId("shopId")
                        .setCreatedAt("2016-03-22T06:12:27Z")
                        .setStatus(InvoiceStatus.unpaid(new InvoiceUnpaid()))
                        .setDetails(new InvoiceDetails()
                                .setProduct("product")
                                .setCart(new InvoiceCart()
                                        .setLines(Arrays.asList(
                                                new InvoiceLine()
                                                        .setQuantity(1)
                                                        .setProduct("product")
                                                        .setPrice(new Cash(1L, new CurrencyRef("RUB")))))))
                        .setDue("2016-03-22T06:12:27Z")
                        .setCost(new Cash()
                                .setAmount(123L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB")))
                        .setContext(new Content()));
        ic.setInvoiceCreated(invoiceCreated);
        return ic;
    }
}
