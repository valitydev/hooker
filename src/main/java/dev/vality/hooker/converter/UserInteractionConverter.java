package dev.vality.hooker.converter;

import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.interaction.*;
import dev.vality.hooker.model.interaction.PaymentTerminalReceipt;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.*;
import lombok.RequiredArgsConstructor;
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

    private static QrCodeDisplayRequest createQrCodeDisplayRequest(QrCodeDisplay qrCodeDisplay) {
        QrCodeDisplayRequest qrCodeDisplayRequest = new QrCodeDisplayRequest();
        QrCodeDisplayInfo qrCodeDisplayInfo = new QrCodeDisplayInfo();
        qrCodeDisplayInfo.setQrCode(new String(qrCodeDisplay.getQrCode()));
        qrCodeDisplayRequest.setQrCodeDisplayInfo(qrCodeDisplayInfo);
        qrCodeDisplayRequest.setUserInteractionType(
                UserInteractionDetails.UserInteractionTypeEnum.QR_CODE_DISPLAY_REQUEST);
        return qrCodeDisplayRequest;
    }

    private static dev.vality.swag_webhook_events.model.PaymentTerminalReceipt createPaymentTerminalReceipt(
            PaymentTerminalReceipt paymentTerminalReceipt) {
        var paymentTerminalReceiptRequest = new dev.vality.swag_webhook_events.model.PaymentTerminalReceipt();
        PaymentTerminalReceiptInfo paymentTerminalReceiptInfo = new PaymentTerminalReceiptInfo();
        paymentTerminalReceiptInfo.setShortPaymentId(paymentTerminalReceipt.getShortPaymentId());
        paymentTerminalReceiptInfo.setDue(TimeUtils.toOffsetDateTime(paymentTerminalReceipt.getDue()));
        paymentTerminalReceiptRequest.setPaymentTerminalReceiptInfo(paymentTerminalReceiptInfo);
        paymentTerminalReceiptRequest.setUserInteractionType(
                UserInteractionDetails.UserInteractionTypeEnum.PAYMENT_TERMINAL_RECEIPT);
        return paymentTerminalReceiptRequest;
    }

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
                UserInteractionDetails.UserInteractionTypeEnum.CRYPTO_CURRENCY_TRANSFER_REQUEST);
        return cryptoCurrencyTransferRequest;
    }

    private static ApiExtensionRequest createApiExtensionRequest(ApiExtension apiExtension) {
        ApiExtensionRequest apiExtensionRequest = new ApiExtensionRequest();
        ApiExtensionInfo apiExtensionInfo = new ApiExtensionInfo();
        apiExtensionInfo.setApiType(apiExtension.getApiType());
        apiExtensionRequest.setApiExtensionInfo(apiExtensionInfo);
        apiExtensionRequest.setUserInteractionType(
                UserInteractionDetails.UserInteractionTypeEnum.API_EXTENSION_REQUEST);
        return apiExtensionRequest;
    }

    private static BrowserHTTPRequest createBrowserHttpRequest(BrowserHttpInteraction browserHttpInteraction) {
        BrowserHTTPInfo browserHttpInfo = new BrowserHTTPInfo();
        browserHttpInfo.setUrl(browserHttpInteraction.getUrl());
        browserHttpInfo.setRequestType(
                BrowserHTTPInfo.RequestTypeEnum.fromValue(browserHttpInteraction.getRequestType()));
        browserHttpInfo.setForm(browserHttpInteraction.getForm());
        BrowserHTTPRequest browserHttpRequest = new BrowserHTTPRequest();
        browserHttpRequest.setBrowserHTTPInfo(browserHttpInfo);
        browserHttpRequest.setUserInteractionType(UserInteractionDetails.UserInteractionTypeEnum.BROWSER_HTTP_REQUEST);
        return browserHttpRequest;
    }
}
