package dev.vality.hooker.converter;

import dev.vality.damsel.domain.AdditionalTransactionInfo;
import dev.vality.damsel.domain.DisposablePaymentResource;
import dev.vality.damsel.domain.InvoicePaymentCaptured;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.hooker.model.ExpandedPayment;
import dev.vality.hooker.model.FeeType;
import dev.vality.hooker.utils.CashFlowUtils;
import dev.vality.hooker.utils.ErrorUtils;
import dev.vality.hooker.utils.PaymentToolUtils;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentConverter {

    private final MetadataDeserializer deserializer;

    public ExpandedPayment convert(InvoicePayment sourceWrapper, Invoice invoice) {
        var source = sourceWrapper.getPayment();
        ExpandedPayment target = new ExpandedPayment();
        target.setId(source.getId());
        target.setCreatedAt(TimeUtils.toOffsetDateTime(source.getCreatedAt()));
        target.setStatus(Payment.StatusEnum.fromValue(source.getStatus().getSetField().getFieldName()));
        if (source.isSetChangedCost()) {
            target.setChangedAmount(source.getChangedCost().getAmount());
            target.setAmount(invoice.getInvoice().getCost().getAmount());
        } else {
            target.setAmount(source.getCost().getAmount());
        }
        target.setCurrency(source.getCost().getCurrency().getSymbolicCode());
        target.setMetadata(getMetadata(source));
        target.setFee(getFee(sourceWrapper));
        target.setRrn(getRrn(sourceWrapper));
        target.setExtraPaymentInfo(getExtraPaymentInfo(sourceWrapper));

        if (source.getStatus().isSetFailed()) {
            setErrorDetails(source, target);
        } else if (source.getStatus().isSetCaptured()) {
            setCapturedParams(source, target);
        }

        if (source.getPayer().isSetPaymentResource()) {
            setResourcePaymentTool(source, target);
        } else if (source.getPayer().isSetCustomer()) {
            setCustomerPaymentTool(source, target);
        } else if (source.getPayer().isSetRecurrent()) {
            setRecurrentPaymentTool(source, target);
        }

        target.setExternalId(source.getExternalId());
        return target;
    }

    private Object getMetadata(dev.vality.damsel.domain.InvoicePayment source) {
        return source.isSetContext() ? deserializer.deserialize(source.getContext().getData()) : null;
    }

    private Long getFee(InvoicePayment sourceWrapper) {
        return sourceWrapper.isSetCashFlow()
                ? CashFlowUtils.getFees(sourceWrapper.getCashFlow()).getOrDefault(FeeType.FEE, 0L) : 0L;
    }

    private String getRrn(InvoicePayment sourceWrapper) {
        return isSetAdditionalInfo(sourceWrapper) ? getAdditionalInfo(sourceWrapper).getRrn() : null;
    }

    private Map<String, String> getExtraPaymentInfo(InvoicePayment sourceWrapper) {
        return isSetAdditionalInfo(sourceWrapper) ? getAdditionalInfo(sourceWrapper).getExtraPaymentInfo() : null;
    }

    private void setErrorDetails(dev.vality.damsel.domain.InvoicePayment source, Payment target) {
        target.setError(ErrorUtils.getPaymentError(source.getStatus().getFailed().getFailure()));
    }

    private void setCapturedParams(dev.vality.damsel.domain.InvoicePayment source, Payment target) {
        InvoicePaymentCaptured invoicePaymentCaptured = source.getStatus().getCaptured();
        if (invoicePaymentCaptured.isSetCost()) {
            target.setCurrency(invoicePaymentCaptured.getCost().getCurrency().getSymbolicCode());
        }
    }

    private void setResourcePaymentTool(dev.vality.damsel.domain.InvoicePayment source, Payment target) {
        dev.vality.damsel.domain.PaymentResourcePayer payerOrigin = source.getPayer().getPaymentResource();
        DisposablePaymentResource resourceOrigin = payerOrigin.getResource();
        PaymentTool paymentTool = resourceOrigin.getPaymentTool();
        target.paymentToolToken(PaymentToolUtils.getPaymentToolToken(paymentTool))
                .paymentSession(resourceOrigin.getPaymentSessionId())
                .contactInfo(new PaymentContactInfo()
                        .email(payerOrigin.getContactInfo().getEmail())
                        .phoneNumber(payerOrigin.getContactInfo().getPhoneNumber()))
                .ip(resourceOrigin.isSetClientInfo() ? resourceOrigin.getClientInfo().getIpAddress() : null)
                .fingerprint(resourceOrigin.isSetClientInfo() ? resourceOrigin.getClientInfo().getFingerprint() : null)
                .payer(new PaymentResourcePayer()
                        .paymentSession(resourceOrigin.getPaymentSessionId())
                        .paymentToolToken(target.getPaymentToolToken())
                        .contactInfo(new ContactInfo()
                                .email(payerOrigin.getContactInfo().getEmail())
                                .phoneNumber(payerOrigin.getContactInfo().getPhoneNumber()))
                        .clientInfo(new ClientInfo()
                                .ip(resourceOrigin.isSetClientInfo() ? resourceOrigin.getClientInfo().getIpAddress() :
                                        null)
                                .fingerprint(resourceOrigin.isSetClientInfo()
                                        ? resourceOrigin.getClientInfo().getFingerprint() : null))
                        .paymentToolDetails(PaymentToolUtils.getPaymentToolDetails(paymentTool))
                        .payerType(Payer.PayerTypeEnum.PAYMENTRESOURCEPAYER));
    }

    private void setRecurrentPaymentTool(dev.vality.damsel.domain.InvoicePayment source, Payment target) {
        dev.vality.damsel.domain.RecurrentPayer recurrentParentOrigin = source.getPayer().getRecurrent();
        target.contactInfo(new PaymentContactInfo()
                        .email(recurrentParentOrigin.getContactInfo().getEmail())
                        .phoneNumber(recurrentParentOrigin.getContactInfo().getPhoneNumber()))
                .payer(new RecurrentPayer()
                        .recurrentParentPayment(new PaymentRecurrentParent()
                                .invoiceID(recurrentParentOrigin.getRecurrentParent().getInvoiceId())
                                .paymentID(recurrentParentOrigin.getRecurrentParent().getPaymentId()))
                        .contactInfo(new ContactInfo()
                                .email(recurrentParentOrigin.getContactInfo().getEmail())
                                .phoneNumber(recurrentParentOrigin.getContactInfo().getPhoneNumber()))
                        .payerType(Payer.PayerTypeEnum.RECURRENTPAYER))
                .contactInfo(new PaymentContactInfo()
                        .email(recurrentParentOrigin.getContactInfo().getEmail())
                        .phoneNumber(recurrentParentOrigin.getContactInfo().getPhoneNumber()));
    }

    private void setCustomerPaymentTool(dev.vality.damsel.domain.InvoicePayment source, Payment target) {
        dev.vality.damsel.domain.CustomerPayer customerPayerOrigin = source.getPayer().getCustomer();
        target.paymentToolToken(PaymentToolUtils.getPaymentToolToken(customerPayerOrigin.getPaymentTool()))
                .contactInfo(new PaymentContactInfo()
                        .email(customerPayerOrigin.getContactInfo().getEmail())
                        .phoneNumber(customerPayerOrigin.getContactInfo().getPhoneNumber()))
                .payer(new CustomerPayer()
                        .customerID(source.getPayer().getCustomer().getCustomerId())
                        .payerType(Payer.PayerTypeEnum.CUSTOMERPAYER));
    }

    private boolean isSetAdditionalInfo(InvoicePayment sourceWrapper) {
        return (!sourceWrapper.getSessions().isEmpty())
                && sourceWrapper.getSessions().get(0).isSetTransactionInfo()
                && sourceWrapper.getSessions().get(0).getTransactionInfo().isSetAdditionalInfo();
    }

    private AdditionalTransactionInfo getAdditionalInfo(InvoicePayment sourceWrapper) {
        return sourceWrapper.getSessions().get(0).getTransactionInfo().getAdditionalInfo();
    }
}
