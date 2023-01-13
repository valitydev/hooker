package dev.vality.hooker.model;

import dev.vality.swag_webhook_events.model.Payment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
public class ExpandedPayment extends Payment {

    private Map<String, String> extraPaymentInfo;

}
