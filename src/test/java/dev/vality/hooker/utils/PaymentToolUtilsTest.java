package dev.vality.hooker.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.DigitalWallet;
import dev.vality.damsel.domain.LegacyCryptoCurrency;
import dev.vality.damsel.domain.LegacyDigitalWalletProvider;
import dev.vality.damsel.domain.LegacyTerminalPaymentProvider;
import dev.vality.damsel.domain.PaymentTerminal;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.swag_webhook_events.model.PaymentToolDetails;
import dev.vality.swag_webhook_events.model.PaymentToolDetailsBankCard;
import dev.vality.swag_webhook_events.model.PaymentToolDetailsCryptoWallet;
import dev.vality.swag_webhook_events.model.PaymentToolDetailsPaymentTerminal;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentToolUtilsTest {

    @Test
    public void testGetPaymentToolDetailsCryptoWallet() {
        PaymentTool paymentTool = PaymentTool.crypto_currency_deprecated(LegacyCryptoCurrency.bitcoin);
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsCryptoWallet);
        assertEquals(PaymentToolDetails.DetailsTypeEnum.PAYMENTTOOLDETAILSCRYPTOWALLET,
                paymentToolDetails.getDetailsType());
        assertEquals(dev.vality.swag_webhook_events.model.CryptoCurrency.BITCOIN.getValue(),
                ((PaymentToolDetailsCryptoWallet) paymentToolDetails).getCryptoCurrency().getValue());
    }

    @Test
    public void testDigitalWalletJson() throws JsonProcessingException {
        PaymentTool paymentTool =
                PaymentTool.digital_wallet(new DigitalWallet("kke"));
        paymentTool.getDigitalWallet().setProviderDeprecated(LegacyDigitalWalletProvider.qiwi);
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        String json = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(paymentToolDetails);
        assertEquals("{\"digitalWalletDetailsType\":\"DigitalWalletDetailsQIWI\"," +
                        "\"detailsType\":\"PaymentToolDetailsDigitalWallet\"}",
                json);
    }

    @Test
    public void testGetPaymentToolDetailsPaymentTerminal() {
        PaymentTool paymentTool = PaymentTool.payment_terminal(new PaymentTerminal()
                .setTerminalTypeDeprecated(LegacyTerminalPaymentProvider.alipay)
        );
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsPaymentTerminal);
        assertEquals(PaymentToolDetails.DetailsTypeEnum.PAYMENTTOOLDETAILSPAYMENTTERMINAL,
                paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsPaymentTerminal.ProviderEnum.ALIPAY.getValue(),
                ((PaymentToolDetailsPaymentTerminal) paymentToolDetails).getProvider().getValue());

        paymentTool = PaymentTool.payment_terminal(new PaymentTerminal()
                .setTerminalTypeDeprecated(LegacyTerminalPaymentProvider.wechat)
        );
        paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsPaymentTerminal);
        assertEquals(PaymentToolDetails.DetailsTypeEnum.PAYMENTTOOLDETAILSPAYMENTTERMINAL,
                paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsPaymentTerminal.ProviderEnum.WECHAT.getValue(),
                ((PaymentToolDetailsPaymentTerminal) paymentToolDetails).getProvider().getValue());
    }

    @Test
    public void testGetPaymentToolDetailsBankCard() throws IOException {
        PaymentTool paymentTool = PaymentTool.bank_card(new MockTBaseProcessor(MockMode.RANDOM, 15, 2)
                .process(new BankCard(), new TBaseHandler<>(BankCard.class)));
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsBankCard);
        assertNotNull(paymentToolDetails.getDetailsType());
        assertEquals(paymentTool.getBankCard().getBin(), ((PaymentToolDetailsBankCard) paymentToolDetails).getBin());
    }

}
