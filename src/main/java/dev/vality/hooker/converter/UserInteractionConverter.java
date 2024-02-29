package dev.vality.hooker.converter;

import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.interaction.PaymentTerminalReceipt;
import dev.vality.hooker.model.interaction.*;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInteractionConverter implements Converter<InvoicingMessage, UserInteractionDetails> {

    @Override
    public UserInteractionDetails convert(InvoicingMessage invoicingMessage) {
        UserInteractionDetails userInteractionDetails = new UserInteractionDetails();
        UserInteraction interaction = invoicingMessage.getUserInteraction();
        if (interaction instanceof BrowserHttpInteraction browserHttpInteraction) {
            return createBrowserHttpRequest(browserHttpInteraction);
        } else if (interaction instanceof ApiExtension apiExtension) {
            return createApiExtensionRequest(apiExtension);
        } else if (interaction instanceof CryptoCurrencyTransfer cryptoCurrencyTransfer) {
            return createCryptoCurrencyTransferRequest(cryptoCurrencyTransfer);
        } else if (interaction instanceof PaymentTerminalReceipt paymentTerminalReceipt) {
            return createPaymentTerminalReceipt(paymentTerminalReceipt);
        } else if (interaction instanceof QrCodeDisplay qrCodeDisplay) {
            return createQrCodeDisplayRequest(qrCodeDisplay);
        }
        return userInteractionDetails;
    }

    @NotNull
    private static QrCodeDisplayRequest createQrCodeDisplayRequest(QrCodeDisplay qrCodeDisplay) {
        QrCodeDisplayRequest qrCodeDisplayRequest = new QrCodeDisplayRequest();
        QrCodeDisplayInfo qrCodeDisplayInfo = new QrCodeDisplayInfo();
        qrCodeDisplayInfo.setQrCode(new String(qrCodeDisplay.getQrCode()));
        qrCodeDisplayRequest.setQrCodeDisplayInfo(qrCodeDisplayInfo);
        qrCodeDisplayRequest.setUserInteractionType(
                UserInteractionDetails.UserInteractionTypeEnum.QRCODEDISPLAYREQUEST);
        return qrCodeDisplayRequest;
    }

    @NotNull
    private static dev.vality.swag_webhook_events.model.PaymentTerminalReceipt createPaymentTerminalReceipt(
            PaymentTerminalReceipt paymentTerminalReceipt) {
        var paymentTerminalReceiptRequest = new dev.vality.swag_webhook_events.model.PaymentTerminalReceipt();
        PaymentTerminalReceiptInfo paymentTerminalReceiptInfo = new PaymentTerminalReceiptInfo();
        paymentTerminalReceiptInfo.setShortPaymentId(paymentTerminalReceipt.getShortPaymentId());
        paymentTerminalReceiptInfo.setDue(TimeUtils.toOffsetDateTime(paymentTerminalReceipt.getDue()));
        paymentTerminalReceiptRequest.setPaymentTerminalReceiptInfo(paymentTerminalReceiptInfo);
        paymentTerminalReceiptRequest.setUserInteractionType(
                UserInteractionDetails.UserInteractionTypeEnum.PAYMENTTERMINALRECEIPT);
        return paymentTerminalReceiptRequest;
    }

    @NotNull
    private static CryptoCurrencyTransferRequest createCryptoCurrencyTransferRequest(
            CryptoCurrencyTransfer cryptoCurrencyTransfer) {
        CryptoCurrencyTransferInfo cryptoCurrencyTransferInfo = new CryptoCurrencyTransferInfo();
        cryptoCurrencyTransferInfo.setCryptoAddress(cryptoCurrencyTransfer.getCryptoAddress());
        cryptoCurrencyTransferInfo.setCryptoCurrency(cryptoCurrencyTransfer.getCryptoSymbolicCode());
        Rational cryptoAmount = new Rational();
        cryptoAmount.setDenominator(cryptoCurrencyTransfer.getCryptoAmount().getP());
        cryptoAmount.setDivider(cryptoCurrencyTransfer.getCryptoAmount().getQ());
        cryptoCurrencyTransferInfo.setCryptoAmount(cryptoAmount);
        CryptoCurrencyTransferRequest cryptoCurrencyTransferRequest = new CryptoCurrencyTransferRequest();
        cryptoCurrencyTransferRequest.setCryptoCurrencyTransferInfo(cryptoCurrencyTransferInfo);
        cryptoCurrencyTransferRequest.setUserInteractionType(
                UserInteractionDetails.UserInteractionTypeEnum.CRYPTOCURRENCYTRANSFERREQUEST);
        return cryptoCurrencyTransferRequest;
    }

    @NotNull
    private static ApiExtensionRequest createApiExtensionRequest(ApiExtension apiExtension) {
        ApiExtensionRequest apiExtensionRequest = new ApiExtensionRequest();
        ApiExtensionInfo apiExtensionInfo = new ApiExtensionInfo();
        apiExtensionInfo.setApiType(apiExtension.getApiType());
        apiExtensionRequest.setApiExtensionInfo(apiExtensionInfo);
        apiExtensionRequest.setUserInteractionType(UserInteractionDetails.UserInteractionTypeEnum.APIEXTENSIONREQUEST);
        return apiExtensionRequest;
    }

    @NotNull
    private static BrowserHTTPRequest createBrowserHttpRequest(BrowserHttpInteraction browserHttpInteraction) {
        BrowserHTTPInfo browserHttpInfo = new BrowserHTTPInfo();
        browserHttpInfo.setUrl(browserHttpInteraction.getUrl());
        browserHttpInfo.setRequestType(
                BrowserHTTPInfo.RequestTypeEnum.fromValue(browserHttpInteraction.getRequestType()));
        browserHttpInfo.setForm(browserHttpInteraction.getForm());
        BrowserHTTPRequest browserHttpRequest = new BrowserHTTPRequest();
        browserHttpRequest.setBrowserHTTPInfo(browserHttpInfo);
        browserHttpRequest.setUserInteractionType(UserInteractionDetails.UserInteractionTypeEnum.BROWSERHTTPREQUEST);
        return browserHttpRequest;
    }
}
