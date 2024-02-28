package dev.vality.hooker.converter;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.model.interaction.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceChangeToUserInteractionConverter implements Converter<InvoiceChange, UserInteraction> {

    @Override
    public UserInteraction convert(InvoiceChange source) {
        dev.vality.damsel.user_interaction.UserInteraction interaction =
                source.getInvoicePaymentChange().getPayload().getInvoicePaymentSessionChange().getPayload()
                        .getSessionInteractionChanged().interaction;
        dev.vality.hooker.model.interaction.UserInteraction userInteraction = null;
        if (interaction.isSetApiExtensionRequest()) {
            userInteraction = new ApiExtension(interaction.getApiExtensionRequest().getApiType());
        } else if (interaction.isSetCryptoCurrencyTransferRequest()) {
            userInteraction =
                    new CryptoCurrencyTransfer(interaction.getCryptoCurrencyTransferRequest().getCryptoAddress(),
                            interaction.getCryptoCurrencyTransferRequest().getCryptoCash().getCryptoAmount(),
                            interaction.getCryptoCurrencyTransferRequest().getCryptoCash().getCryptoSymbolicCode());
        } else if (interaction.isSetPaymentTerminalReciept()) {
            userInteraction = new PaymentTerminalReceipt(interaction.getPaymentTerminalReciept().getShortPaymentId(),
                    interaction.getPaymentTerminalReciept().getDue());
        } else if (interaction.isSetQrCodeDisplayRequest()) {
            userInteraction = new QrCodeDisplay(interaction.getQrCodeDisplayRequest().getQrCode().getPayload());
        } else if (interaction.isSetRedirect()) {
            if (interaction.getRedirect().isSetPostRequest()) {
                userInteraction = new BrowserHttpInteraction("post",
                        interaction.getRedirect().getPostRequest().getUri(),
                        interaction.getRedirect().getPostRequest().getForm());
            } else {
                userInteraction = new BrowserHttpInteraction("get",
                        interaction.getRedirect().getPostRequest().getUri(), null);
            }
        }
        return userInteraction;
    }
}
