package dev.vality.hooker.converter;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.user_interaction.*;
import dev.vality.hooker.model.interaction.PaymentTerminalReceipt;
import dev.vality.hooker.model.interaction.UserInteraction;
import dev.vality.hooker.model.interaction.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceChangeToUserInteractionConverter implements Converter<InvoiceChange, UserInteraction> {

    @Override
    public UserInteraction convert(InvoiceChange source) {
        var interaction = source.getInvoicePaymentChange().getPayload().getInvoicePaymentSessionChange().getPayload()
                .getSessionInteractionChanged().getInteraction();
        return createUserInteraction(interaction);
    }

    @Nullable
    private static UserInteraction createUserInteraction(
            dev.vality.damsel.user_interaction.UserInteraction interaction) {
        UserInteraction userInteraction = null;
        if (interaction.isSetApiExtensionRequest()) {
            userInteraction = new ApiExtension(interaction.getApiExtensionRequest().getApiType());
        } else if (interaction.isSetCryptoCurrencyTransferRequest()) {
            CryptoCurrencyTransferRequest cryptoCurrencyTransferRequest =
                    interaction.getCryptoCurrencyTransferRequest();
            CryptoCash cryptoCash = cryptoCurrencyTransferRequest.getCryptoCash();
            userInteraction = new CryptoCurrencyTransfer(cryptoCurrencyTransferRequest.getCryptoAddress(),
                    cryptoCash.getCryptoAmount(),
                    cryptoCash.getCryptoSymbolicCode());
        } else if (interaction.isSetPaymentTerminalReciept()) {
            dev.vality.damsel.user_interaction.PaymentTerminalReceipt paymentTerminalReciept =
                    interaction.getPaymentTerminalReciept();
            userInteraction = new PaymentTerminalReceipt(paymentTerminalReciept.getShortPaymentId(),
                    paymentTerminalReciept.getDue());
        } else if (interaction.isSetQrCodeDisplayRequest()) {
            QrCode qrCode = interaction.getQrCodeDisplayRequest().getQrCode();
            userInteraction = new QrCodeDisplay(qrCode.getPayload());
        } else if (interaction.isSetRedirect()) {
            if (interaction.getRedirect().isSetPostRequest()) {
                BrowserPostRequest postRequest = interaction.getRedirect().getPostRequest();
                userInteraction = new BrowserHttpInteraction("post", postRequest.getUri(), postRequest.getForm());
            } else {
                BrowserGetRequest getRequest = interaction.getRedirect().getGetRequest();
                userInteraction = new BrowserHttpInteraction("get", getRequest.getUri(), null);
            }
        }
        return userInteraction;
    }
}
