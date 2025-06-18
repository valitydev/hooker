package dev.vality.hooker.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vality.damsel.domain.*;
import dev.vality.swag_webhook_events.model.*;
import dev.vality.swag_webhook_events.model.CryptoCurrency;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@UtilityClass
public class PaymentToolUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static PaymentToolDetails getPaymentToolDetails(PaymentTool paymentTool) {
        if (paymentTool.isSetBankCard()) {
            PaymentToolDetailsBankCard.TokenProviderEnum tokenProvider =
                    Arrays.stream(PaymentToolDetailsBankCard.TokenProviderEnum.values())
                            .filter(tokenProviderEnum ->
                                    tokenProviderEnum.name().equalsIgnoreCase(
                                            Optional.ofNullable(paymentTool.getBankCard().getPaymentToken())
                                                    .map(BankCardTokenServiceRef::getId).orElse(null)
                                    )
                            )
                            .findFirst().orElse(PaymentToolDetailsBankCard.TokenProviderEnum.UNKNOWN);
            return new PaymentToolDetailsBankCard()
                    .bin(paymentTool.getBankCard().getBin())
                    .lastDigits(paymentTool.getBankCard().getLastDigits())
                    .cardNumberMask(
                            paymentTool.getBankCard().getBin() + "******" + paymentTool.getBankCard().getLastDigits())
                    .tokenProvider(paymentTool.getBankCard().getPaymentToken() != null
                            ? tokenProvider
                            : null)
                    .tokenProviderName(
                            Optional.ofNullable(paymentTool.getBankCard().getPaymentToken())
                                    .map(BankCardTokenServiceRef::getId).orElse(null))

                    .paymentSystem(
                            Optional.ofNullable(paymentTool.getBankCard().getPaymentSystem())
                                    .map(PaymentSystemRef::getId).orElse(null))
                    .issuerCountry(paymentTool.getBankCard().getIssuerCountry() != null
                            ? paymentTool.getBankCard().getIssuerCountry().name()
                            : null)
                    .bankName(paymentTool.getBankCard().getBankName())
                    .detailsType(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_BANK_CARD);
        } else if (paymentTool.isSetPaymentTerminal()) {
            PaymentToolDetailsPaymentTerminal.ProviderEnum provider =
                    Arrays.stream(PaymentToolDetailsPaymentTerminal.ProviderEnum.values())
                            .filter(providerEnum ->
                                    providerEnum.name().equalsIgnoreCase(
                                            Optional.ofNullable(paymentTool.getPaymentTerminal().getPaymentService())
                                                    .map(PaymentServiceRef::getId).orElse(null))
                            )
                            .findFirst().orElse(PaymentToolDetailsPaymentTerminal.ProviderEnum.UNKNOWN);
            return new PaymentToolDetailsPaymentTerminal()
                    .provider(provider)
                    .providerName(paymentTool.getPaymentTerminal().getPaymentService().getId())
                    .detailsType(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_PAYMENT_TERMINAL);
        } else if (paymentTool.isSetDigitalWallet()) {
            if ("qiwi".equals(Optional.ofNullable(paymentTool.getDigitalWallet().getPaymentService())
                    .map(PaymentServiceRef::getId).orElse(null))) {
                return new PaymentToolDetailsDigitalWallet()
                        .digitalWalletDetailsType(
                                PaymentToolDetailsDigitalWallet
                                        .DigitalWalletDetailsTypeEnum
                                        .DIGITAL_WALLET_DETAILS_QIWI)
                        .detailsType(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_DIGITAL_WALLET);
            } else {
                throw new UnsupportedOperationException("Unknown digital wallet type");
            }
        } else if (paymentTool.isSetCryptoCurrency()) {
            CryptoCurrency cryptoCurrency = Arrays.stream(CryptoCurrency.values())
                    .filter(crypto -> crypto.name().equalsIgnoreCase(paymentTool.getCryptoCurrency().getId()))
                    .findFirst().orElse(CryptoCurrency.UNKNOWN);
            return new PaymentToolDetailsCryptoWallet()
                    .cryptoCurrency(cryptoCurrency)
                    .cryptoCurrencyType(paymentTool.getCryptoCurrency().getId())
                    .detailsType(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_CRYPTO_WALLET);
        } else if (paymentTool.isSetMobileCommerce()) {
            return new PaymentToolDetailsMobileCommerce()
                    .phoneNumber(paymentTool.getMobileCommerce().getPhone().getCc() +
                            paymentTool.getMobileCommerce().getPhone().getCtn())
                    .detailsType(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_MOBILE_COMMERCE);
        } else {
            throw new UnsupportedOperationException(
                    "Unknown payment tool type " + paymentTool);
        }
    }

    public static String getPaymentToolToken(PaymentTool paymentTool) {
        ObjectNode rootNode = mapper.createObjectNode();
        if (paymentTool.isSetBankCard()) {
            BankCard paymentCard = paymentTool.getBankCard();
            rootNode.put("type", "bank_card");
            rootNode.put("token", paymentCard.getToken());
            rootNode.put(
                    "payment_system",
                    Optional.ofNullable(paymentCard.getPaymentSystem()).map(PaymentSystemRef::getId).orElse(null)
            );
            rootNode.put("bin", paymentCard.getBin());
            rootNode.put("masked_pan", paymentCard.getLastDigits());
            rootNode.put(
                    "token_provider",
                    Optional.ofNullable(paymentCard.getPaymentToken()).map(BankCardTokenServiceRef::getId).orElse(null)
            );
        } else if (paymentTool.isSetPaymentTerminal()) {
            rootNode.put("type", "payment_terminal");
            rootNode.put(
                    "terminal_type",
                    paymentTool.getPaymentTerminal().getPaymentService().getId()
            );
        } else if (paymentTool.isSetDigitalWallet()) {
            rootNode.put("type", "digital_wallet");
            rootNode.put("provider", paymentTool.getDigitalWallet().getPaymentService().getId());
            rootNode.put("id", paymentTool.getDigitalWallet().getId());
        } else if (paymentTool.isSetCryptoCurrency()) {
            rootNode.put("type", "crypto_currency");
            rootNode.put("crypto_currency", paymentTool.getCryptoCurrency().getId());
        } else if (paymentTool.isSetMobileCommerce()) {
            rootNode.put("type", "mobile_commerce");
            MobilePhone mobilePhone = paymentTool.getMobileCommerce().getPhone();
            rootNode.put("mobile_commerce", mobilePhone.getCc() + mobilePhone.getCtn());
        } else {
            throw new UnsupportedOperationException(
                    "Unknown payment tool type " + paymentTool);
        }
        return Base64.getUrlEncoder().encodeToString(rootNode.toString().getBytes(StandardCharsets.UTF_8));
    }
}
