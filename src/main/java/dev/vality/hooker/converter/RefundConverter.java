package dev.vality.hooker.converter;

import dev.vality.damsel.domain.AdditionalTransactionInfo;
import dev.vality.damsel.payment_processing.InvoicePaymentRefund;
import dev.vality.hooker.model.FeeType;
import dev.vality.hooker.utils.CashFlowUtils;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.Refund;
import dev.vality.swag_webhook_events.model.RefundError;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import static dev.vality.hooker.utils.ErrorUtils.getRefundError;

@Component
public class RefundConverter implements Converter<InvoicePaymentRefund, Refund> {

    @Override
    public Refund convert(InvoicePaymentRefund sourceWrapper) {
        var source = sourceWrapper.getRefund();

        return new Refund()
                .id(source.getId())
                .createdAt(TimeUtils.toOffsetDateTime(source.getCreatedAt()))
                .reason(source.getReason())
                .status(Refund.StatusEnum.fromValue(source.getStatus().getSetField().getFieldName()))
                .error(getError(source))
                .amount(getAmount(sourceWrapper))
                .currency(getCurrency(sourceWrapper))
                .rrn(getRrn(sourceWrapper));
    }

    private RefundError getError(dev.vality.damsel.domain.InvoicePaymentRefund source) {
        return source.getStatus().isSetFailed() ? getRefundError(source.getStatus().getFailed().getFailure()) : null;
    }

    private Long getAmount(InvoicePaymentRefund sourceWrapper) {
        if (sourceWrapper.getRefund().isSetCash()) {
            return sourceWrapper.getRefund().getCash().getAmount();
        }
        if (sourceWrapper.isSetCashFlow()) {
            return CashFlowUtils.getFees(sourceWrapper.getCashFlow()).getOrDefault(FeeType.AMOUNT, null);
        }
        return null;
    }

    private String getCurrency(InvoicePaymentRefund sourceWrapper) {
        if (sourceWrapper.getRefund().isSetCash()) {
            return sourceWrapper.getRefund().getCash().getCurrency().getSymbolicCode();
        }
        if (sourceWrapper.isSetCashFlow()) {
            return CashFlowUtils.getCurrency(sourceWrapper.getCashFlow()).getOrDefault(FeeType.AMOUNT, null);
        }
        return null;
    }

    private String getRrn(InvoicePaymentRefund sourceWrapper) {
        return isSetAdditionalInfo(sourceWrapper) ? getAdditionalInfo(sourceWrapper).getRrn() : null;
    }

    private boolean isSetAdditionalInfo(InvoicePaymentRefund damselRefund) {
        return (!damselRefund.getSessions().isEmpty())
                && damselRefund.getSessions().get(0).isSetTransactionInfo()
                && damselRefund.getSessions().get(0).getTransactionInfo().isSetAdditionalInfo();
    }

    private AdditionalTransactionInfo getAdditionalInfo(InvoicePaymentRefund damselRefund) {
        return damselRefund.getSessions().get(0).getTransactionInfo().getAdditionalInfo();
    }
}
