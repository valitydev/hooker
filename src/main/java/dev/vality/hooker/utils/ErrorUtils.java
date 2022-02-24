package dev.vality.hooker.utils;

import dev.vality.damsel.domain.Failure;
import dev.vality.damsel.domain.OperationFailure;
import dev.vality.damsel.domain.SubFailure;
import dev.vality.swag_webhook_events.model.CustomerBindingError;
import dev.vality.swag_webhook_events.model.PaymentError;
import dev.vality.swag_webhook_events.model.RefundError;
import dev.vality.swag_webhook_events.model.SubError;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ErrorUtils {

    public static PaymentError getPaymentError(OperationFailure operationFailure) {
        if (operationFailure.isSetFailure()) {
            Failure failure = operationFailure.getFailure();
            PaymentError paymentError = new PaymentError();
            paymentError.setCode(failure.getCode());
            paymentError.setMessage(failure.getCode());
            if (failure.isSetSub()) {
                SubFailure sub = failure.getSub();
                paymentError.setSubError(getSubError(sub));
            }
            return paymentError;
        } else if (operationFailure.isSetOperationTimeout()) {
            PaymentError paymentError = new PaymentError();
            paymentError.setCode("408");
            paymentError.setMessage("Operation timeout");
            return paymentError;
        }
        return null;
    }

    public static RefundError getRefundError(OperationFailure operationFailure) {
        if (operationFailure.isSetFailure()) {
            Failure failure = operationFailure.getFailure();
            RefundError refundError = new RefundError();
            refundError.setCode(failure.getCode());
            refundError.setMessage(failure.getCode());
            return refundError;
        } else if (operationFailure.isSetOperationTimeout()) {
            RefundError refundError = new RefundError();
            refundError.setCode("408");
            refundError.setMessage("Operation timeout");
            return refundError;
        }
        return null;
    }

    public static CustomerBindingError getCustomerBindingError(OperationFailure failure) {
        String errCode = null;
        String errMess = null;
        if (failure.isSetFailure()) {
            Failure external = failure.getFailure();
            errCode = external.getCode();
            errMess = external.getReason();
        } else if (failure.isSetOperationTimeout()) {
            errCode = "408";
            errMess = "Operation timeout";
        }
        return new CustomerBindingError().code(errCode).message(errMess);
    }

    private static SubError getSubError(SubFailure sub) {
        SubError paymentErrorSubError = new SubError();
        paymentErrorSubError.setCode(sub.getCode());
        if (sub.isSetSub()) {
            paymentErrorSubError.setSubError(getSubError(sub.getSub()));
        }
        return paymentErrorSubError;
    }

    public static String toStringFailure(PaymentError paymentError) {
        StringBuilder sb = new StringBuilder(paymentError.getCode());
        SubError subError = paymentError.getSubError();
        while (subError != null) {
            sb.append(":").append(subError.getCode());
            subError = subError.getSubError();
        }
        return sb.toString();
    }

    public static PaymentError toPaymentError(String failure, String failureReason) {
        String[] codes = failure.split(":");

        return new PaymentError().code(codes[0]).message(failureReason).subError(getSubErrorTree(codes));
    }

    private static SubError getSubErrorTree(String[] codes) {
        if (codes.length == 1) {
            return null;
        }

        List<SubError> subErrors = Arrays.stream(codes)
                .map(code -> new SubError().code(code))
                .collect(Collectors.toList());

        for (int i = 1; i < subErrors.size() - 1; i++) {
            subErrors.get(i).setSubError(subErrors.get(i + 1));
        }

        return subErrors.get(1);
    }
}
