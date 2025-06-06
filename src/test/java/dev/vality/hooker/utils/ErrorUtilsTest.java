package dev.vality.hooker.utils;

import dev.vality.damsel.domain.Failure;
import dev.vality.damsel.domain.OperationFailure;
import dev.vality.damsel.domain.OperationTimeout;
import dev.vality.damsel.domain.SubFailure;
import dev.vality.swag_webhook_events.model.PaymentError;
import dev.vality.swag_webhook_events.model.SubError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErrorUtilsTest {

    @Test
    void getPaymentError() {
        OperationFailure operationFailure = new OperationFailure();
        operationFailure.setOperationTimeout(new OperationTimeout());
        assertEquals("408", ErrorUtils.getPaymentError(operationFailure).getCode());

        OperationFailure operationFailure1 = new OperationFailure();
        Failure value = new Failure();
        operationFailure1.setFailure(value);
        value.setCode("code");
        value.setReason("reason");
        SubFailure sub = new SubFailure();
        value.setSub(sub);
        sub.setCode("sub_code");
        assertEquals("sub_code", ErrorUtils.getPaymentError(operationFailure1).getSubError().getCode());
    }

    @Test
    void toStringFailure() {
        PaymentError paymentError = new PaymentError();
        paymentError.setCode("code");
        paymentError.setMessage("mess");
        SubError subError = new SubError();
        subError.setCode("sub_error");
        paymentError.setSubError(subError);
        assertEquals("code:sub_error", ErrorUtils.toStringFailure(paymentError));
    }

    @Test
    void toPaymentError() {
        assertEquals("test",
                ErrorUtils.toPaymentError("code:sub_code:test", "message").getSubError().getSubError().getCode());
        assertEquals("sub_code", ErrorUtils.toPaymentError("code:sub_code", "message").getSubError().getCode());
        assertNull(ErrorUtils.toPaymentError("code:sub_code", "message").getSubError().getSubError());
        assertEquals("code", ErrorUtils.toPaymentError("code", "message").getCode());
    }
}
