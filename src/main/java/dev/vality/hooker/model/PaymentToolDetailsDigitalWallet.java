package dev.vality.hooker.model;

import dev.vality.swag_webhook_events.model.DigitalWalletDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentToolDetailsDigitalWallet
        extends dev.vality.swag_webhook_events.model.PaymentToolDetailsDigitalWallet {
    private DigitalWalletDetails digitalWalletDetails;
}
