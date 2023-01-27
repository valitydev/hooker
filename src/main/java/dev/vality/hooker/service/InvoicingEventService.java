package dev.vality.hooker.service;

import dev.vality.damsel.payment_processing.EventRange;
import dev.vality.damsel.payment_processing.InvoiceNotFound;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.hooker.converter.InvoiceConverter;
import dev.vality.hooker.converter.PaymentConverter;
import dev.vality.hooker.converter.RefundConverter;
import dev.vality.hooker.exception.NotFoundException;
import dev.vality.hooker.exception.RemoteHostException;
import dev.vality.hooker.model.ExpandedPayment;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.*;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoicingEventService
        implements EventService<InvoicingMessage>, HellgateInvoicingService<InvoicingMessage> {

    private final InvoicingSrv.Iface invoicingClient;
    private final InvoiceConverter invoiceConverter;
    private final PaymentConverter paymentConverter;
    private final RefundConverter refundConverter;

    @Override
    public Event getEventByMessage(InvoicingMessage message) {
        return resolveEvent(message, getInvoiceByMessage(message))
                .eventID(message.getId().intValue())
                .occuredAt(TimeUtils.toOffsetDateTime(message.getEventTime()))
                .topic(Event.TopicEnum.INVOICESTOPIC);
    }

    @Override
    public InvoicePayment getPaymentByMessage(InvoicingMessage message) {
        return extractPayment(message, getInvoiceByMessage(message));
    }

    @Override
    public dev.vality.damsel.payment_processing.Invoice getInvoiceByMessage(InvoicingMessage message) {
        try {
            return invoicingClient.get(message.getSourceId(), getEventRange(message));
        } catch (InvoiceNotFound e) {
            throw new NotFoundException("Invoice not found, invoiceId=" + message.getSourceId());
        } catch (TException e) {
            throw new RemoteHostException(e);
        }
    }

    private EventRange getEventRange(InvoicingMessage message) {
        return new EventRange().setLimit(message.getSequenceId().intValue());
    }

    private Event resolveEvent(InvoicingMessage m, dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        return switch (m.getEventType()) {
            case INVOICE_CREATED -> new InvoiceCreated()
                    .invoice(getSwagInvoice(invoiceInfo))
                    .eventType(Event.EventTypeEnum.INVOICECREATED);
            case INVOICE_STATUS_CHANGED -> resolveInvoiceStatusChanged(m, invoiceInfo);
            case INVOICE_PAYMENT_STARTED -> new PaymentStarted()
                    .invoice(getSwagInvoice(invoiceInfo))
                    .payment(getSwagPayment(m, invoiceInfo))
                    .eventType(Event.EventTypeEnum.PAYMENTSTARTED);
            case INVOICE_PAYMENT_STATUS_CHANGED -> resolvePaymentStatusChanged(m, invoiceInfo);
            case INVOICE_PAYMENT_REFUND_STARTED -> new RefundCreated()
                    .invoice(getSwagInvoice(invoiceInfo))
                    .payment(getSwagPayment(m, invoiceInfo))
                    .refund(getSwagRefund(m, invoiceInfo))
                    .eventType(Event.EventTypeEnum.REFUNDCREATED);
            case INVOICE_PAYMENT_REFUND_STATUS_CHANGED -> resolveRefundStatusChanged(m, invoiceInfo);
            default -> throw new UnsupportedOperationException("Unknown event type " + m.getEventType());
        };
    }

    private Invoice getSwagInvoice(dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        return invoiceConverter.convert(invoiceInfo.getInvoice());
    }

    private Event resolveInvoiceStatusChanged(InvoicingMessage message,
                                              dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        Invoice swagInvoice = getSwagInvoice(invoiceInfo);
        return switch (message.getInvoiceStatus()) {
            case UNPAID -> new InvoiceCreated()
                    .invoice(swagInvoice)
                    .eventType(Event.EventTypeEnum.INVOICECREATED);
            case PAID -> new InvoicePaid()
                    .invoice(swagInvoice)
                    .eventType(Event.EventTypeEnum.INVOICEPAID);
            case CANCELLED -> new InvoiceCancelled()
                    .invoice(swagInvoice)
                    .eventType(Event.EventTypeEnum.INVOICECANCELLED);
            case FULFILLED -> new InvoiceFulfilled()
                    .invoice(swagInvoice)
                    .eventType(Event.EventTypeEnum.INVOICEFULFILLED);
            default -> throw new UnsupportedOperationException("Unknown invoice status " + message.getInvoiceStatus());
        };
    }

    private ExpandedPayment getSwagPayment(InvoicingMessage m,
                                           dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        var damselPayment = extractPayment(m, invoiceInfo);

        return paymentConverter.convert(damselPayment);
    }

    private InvoicePayment extractPayment(InvoicingMessage message,
                                          dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        return invoiceInfo.getPayments().stream()
                .filter(p -> p.getPayment().getId().equals(message.getPaymentId()))
                .findFirst()
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format("Payment not found, invoiceId=%s, paymentId=%s", message.getSourceId(),
                                        message.getPaymentId())
                        )
                );
    }

    private Event resolvePaymentStatusChanged(InvoicingMessage message,
                                              dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        Invoice swagInvoice = getSwagInvoice(invoiceInfo);
        ExpandedPayment swagPayment = getSwagPayment(message, invoiceInfo);
        return switch (message.getPaymentStatus()) {
            case PENDING -> new PaymentStarted()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .eventType(Event.EventTypeEnum.PAYMENTSTARTED);
            case PROCESSED -> new PaymentProcessed()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .eventType(Event.EventTypeEnum.PAYMENTPROCESSED);
            case CAPTURED -> new PaymentCaptured()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .eventType(Event.EventTypeEnum.PAYMENTCAPTURED);
            case CANCELLED -> new PaymentCancelled()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .eventType(Event.EventTypeEnum.PAYMENTCANCELLED);
            case REFUNDED -> new PaymentRefunded()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .eventType(Event.EventTypeEnum.PAYMENTREFUNDED);
            case FAILED -> new PaymentFailed()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .eventType(Event.EventTypeEnum.PAYMENTFAILED);
            default -> throw new UnsupportedOperationException("Unknown payment status " + message.getPaymentStatus());
        };
    }

    private Refund getSwagRefund(InvoicingMessage m, dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        var damselPayment = extractPayment(m, invoiceInfo);
        var damselRefund = extractRefund(m, damselPayment);

        Refund swagRefund = refundConverter.convert(damselRefund);
        if (swagRefund.getAmount() == null) {
            swagRefund.setAmount(damselPayment.getPayment().getCost().getAmount());
            swagRefund.setCurrency(damselPayment.getPayment().getCost().getCurrency().getSymbolicCode());
        }
        return swagRefund;
    }

    private dev.vality.damsel.payment_processing.InvoicePaymentRefund extractRefund(InvoicingMessage m,
                                                                                    InvoicePayment damselPayment) {
        return damselPayment.getRefunds().stream()
                .filter(invoicePaymentRefund -> invoicePaymentRefund.getRefund().getId().equals(m.getRefundId()))
                .findFirst()
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format(
                                        "Refund not found, invoiceId=%s, paymentId=%s, refundId=%s",
                                        m.getSourceId(), m.getPaymentId(), m.getRefundId()
                                )
                        )
                );
    }

    private Event resolveRefundStatusChanged(InvoicingMessage message,
                                             dev.vality.damsel.payment_processing.Invoice invoiceInfo) {
        Invoice swagInvoice = getSwagInvoice(invoiceInfo);
        ExpandedPayment swagPayment = getSwagPayment(message, invoiceInfo);
        Refund swagRefund = getSwagRefund(message, invoiceInfo);
        return switch (message.getRefundStatus()) {
            case PENDING -> new RefundCreated()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .refund(swagRefund)
                    .eventType(Event.EventTypeEnum.REFUNDPENDING);
            case SUCCEEDED -> new RefundSucceeded()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .refund(swagRefund)
                    .eventType(Event.EventTypeEnum.REFUNDSUCCEEDED);
            case FAILED -> new RefundFailed()
                    .invoice(swagInvoice)
                    .payment(swagPayment)
                    .refund(swagRefund)
                    .eventType(Event.EventTypeEnum.REFUNDFAILED);
            default -> throw new UnsupportedOperationException("Unknown refund status " + message.getRefundStatus());
        };
    }
}
