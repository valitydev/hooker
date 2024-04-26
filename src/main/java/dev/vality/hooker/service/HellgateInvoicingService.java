package dev.vality.hooker.service;

import dev.vality.damsel.domain.InvoicePaymentAdjustment;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.hooker.model.Message;

public interface HellgateInvoicingService<M extends Message> {

    Invoice getInvoiceByMessage(M message);

    InvoicePayment getPaymentByMessage(M message);

    InvoicePaymentAdjustment getAdjustmentByMessage(M message);

}
