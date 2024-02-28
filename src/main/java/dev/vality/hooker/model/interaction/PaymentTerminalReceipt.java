package dev.vality.hooker.model.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentTerminalReceipt implements UserInteraction {

    private String shortPaymentId;
    private String due;

}
