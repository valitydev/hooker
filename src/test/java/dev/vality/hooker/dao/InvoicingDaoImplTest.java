package dev.vality.hooker.dao;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.InvoicingDaoImpl;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.InvoiceStatusEnum;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageEnum;
import dev.vality.hooker.model.InvoicingMessageKey;
import dev.vality.hooker.model.PaymentStatusEnum;
import dev.vality.hooker.utils.BuildUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class InvoicingDaoImplTest {

    @Autowired
    private InvoicingDaoImpl messageDao;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                "1234", "56678", EventType.INVOICE_CREATED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.CAPTURED));
        messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                "1235", "56678", EventType.INVOICE_CREATED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.CAPTURED));
        messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.PAYMENT.getValue(),
                "1236", "56678", EventType.INVOICE_CREATED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.CAPTURED));

    }

    @Test
    public void get() throws Exception {
        InvoicingMessage message = messageDao.getInvoicingMessage(
                InvoicingMessageKey.builder().invoiceId("1235").type(InvoicingMessageEnum.INVOICE).build());
        assertEquals(message.getSourceId(), "1235");
        assertEquals(message.getInvoiceStatus(), InvoiceStatusEnum.PAID);

        assertEquals(1, jdbcTemplate.queryForList("select 1 from hook.message where id=:id",
                Map.of("id", message.getId()), Integer.class).size());

        InvoicingMessage payment = messageDao.getInvoicingMessage(
                InvoicingMessageKey.builder().invoiceId("1236").paymentId("123").type(InvoicingMessageEnum.PAYMENT)
                        .build());
        assertEquals("123", payment.getPaymentId());
    }
}
