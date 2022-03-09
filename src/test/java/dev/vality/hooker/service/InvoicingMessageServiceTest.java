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
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.converter.WebhookMessageBuilder;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.dao.impl.InvoicingDaoImpl;
import dev.vality.hooker.handler.invoicing.AbstractInvoiceEventMapper;
import dev.vality.hooker.model.*;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.hooker.utils.KeyUtils;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.validation.constraints.NotNull;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@PostgresqlSpringBootITest
public class InvoicingMessageServiceTest {

    @Autowired
    private MessageService<InvoicingMessage> invoicingService;

    @Autowired
    private HookDao hookDao;

    @MockBean
    private InvoicingEventService invoicingMessageEventService;

    @MockBean
    private WebhookMessageBuilder webhookMessageBuilder;

    @MockBean
    private WebhookKafkaProducerService webhookKafkaProducerService;

    private static final String PARTY_ID = "partyId";

    @BeforeEach
    public void setUp() {
        Mockito.when(invoicingMessageEventService.getEventByMessage(any())).thenReturn(new Event());
        Mockito.when(webhookMessageBuilder.build(any(), any(), any(), any())).thenReturn(new WebhookMessage());
        Mockito.doNothing().when(webhookKafkaProducerService).send(any());
    }

    @Test
    public void testProcess() {
        hookDao.create(BuildUtils.buildHook(PARTY_ID, "www.kek.ru", EventType.INVOICE_CREATED));
        hookDao.create(BuildUtils.buildHook(PARTY_ID, "www.lol.ru", EventType.INVOICE_CREATED));
        InvoicingMessage invoicingMessage = buildMessage(PARTY_ID, "invoice_id",
                InvoicingMessageEnum.INVOICE, EventType.INVOICE_CREATED);
        invoicingService.process(invoicingMessage);
        Mockito.verify(invoicingMessageEventService, Mockito.times(1)).getEventByMessage(any());
        Mockito.verify(webhookMessageBuilder, Mockito.times(2)).build(any(), any(), any(), any());
        Mockito.verify(webhookKafkaProducerService, Mockito.times(2)).send(any());
    }

    private InvoicingMessage buildMessage(String partyId, String invoiceId,
                                          InvoicingMessageEnum type, EventType eventType) {
        InvoicingMessage invoicingMessage = new InvoicingMessage();
        invoicingMessage.setSequenceId(1L);
        invoicingMessage.setChangeId(1);
        invoicingMessage.setEventTime("2016-03-22T06:12:27Z");
        invoicingMessage.setSourceId(invoiceId);
        invoicingMessage.setPartyId(partyId);
        invoicingMessage.setShopId("shopId");
        invoicingMessage.setEventType(eventType);
        invoicingMessage.setType(type);
        invoicingMessage.setInvoiceStatus(InvoiceStatusEnum.UNPAID);
        return invoicingMessage;
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
                        .setOwnerId(PARTY_ID)
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
                                                        .setPrice(new Cash()
                                                                .setAmount(1L)
                                                                .setCurrency(new CurrencyRef("RUB")))))))
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
