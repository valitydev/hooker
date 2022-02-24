package dev.vality.hooker.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.domain.*;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.mamsel.TokenProviderUtil;
import dev.vality.swag_webhook_events.model.PaymentToolDetails;
import dev.vality.swag_webhook_events.model.PaymentToolDetailsBankCard;
import dev.vality.swag_webhook_events.model.PaymentToolDetailsCryptoWallet;
import dev.vality.swag_webhook_events.model.PaymentToolDetailsPaymentTerminal;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PaymentToolUtilsTest {

    //TODO Bump swag-webhook-events
    @Test
    @Ignore
    public void testFromValueWithNull() {
        assertNull(PaymentToolDetailsBankCard.TokenProviderEnum.fromValue(null));
    }

    @Test
    public void testGetPaymentToolDetailsCryptoWallet() {
        PaymentTool paymentTool = PaymentTool.crypto_currency_deprecated(LegacyCryptoCurrency.bitcoin);
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsCryptoWallet);
        assertNull(paymentToolDetails.getDetailsType());
        assertEquals(dev.vality.swag_webhook_events.model.CryptoCurrency.BITCOIN.getValue(),
                ((PaymentToolDetailsCryptoWallet) paymentToolDetails).getCryptoCurrency().getValue());
    }

    //TODO Bump swag-webhook-events
    @Test
    @Ignore
    public void testDigitalWalletJson() throws JsonProcessingException {
        PaymentTool paymentTool =
                PaymentTool.digital_wallet(new DigitalWallet("kke"));
        paymentTool.getDigitalWallet().setProviderDeprecated(LegacyDigitalWalletProvider.qiwi);
        paymentTool.getDigitalWallet().setPaymentService(new PaymentServiceRef("qiwi"));
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        String json = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .writeValueAsString(paymentToolDetails);
        assertEquals(
                "{\"detailsType\":\"PaymentToolDetailsDigitalWallet\",\"digitalWalletDetails\":" +
                        "{\"digitalWalletDetailsType\":\"DigitalWalletDetailsQIWI\",\"phoneNumberMask\":\"kke\"}}",
                json);
    }

    @Test
    public void testGetPaymentToolDetailsPaymentTerminal() {
        PaymentTool paymentTool = PaymentTool.payment_terminal(new PaymentTerminal()
                .setTerminalTypeDeprecated(LegacyTerminalPaymentProvider.alipay)
        );
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsPaymentTerminal);
        assertNull(paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsPaymentTerminal.ProviderEnum.ALIPAY.getValue(),
                ((PaymentToolDetailsPaymentTerminal) paymentToolDetails).getProvider().getValue());

        paymentTool = PaymentTool.payment_terminal(new PaymentTerminal()
                .setTerminalTypeDeprecated(LegacyTerminalPaymentProvider.wechat)
        );
        paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsPaymentTerminal);
        assertNull(paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsPaymentTerminal.ProviderEnum.WECHAT.getValue(),
                ((PaymentToolDetailsPaymentTerminal) paymentToolDetails).getProvider().getValue());
    }

    @Test
    public void testGetPaymentToolDetailsBankCard() throws IOException {
        PaymentTool paymentTool = PaymentTool.bank_card(new MockTBaseProcessor(MockMode.RANDOM, 15, 2)
                .process(new BankCard()
                                .setPaymentSystem(
                                        new PaymentSystemRef(random(LegacyBankCardPaymentSystem.class).name())
                                )
                                .setPaymentToken(
                                        new BankCardTokenServiceRef(random(LegacyBankCardTokenProvider.class).name())
                                ),
                        new TBaseHandler<>(BankCard.class)));
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertTrue(paymentToolDetails instanceof PaymentToolDetailsBankCard);
        assertNull(paymentToolDetails.getDetailsType());
        assertEquals(paymentTool.getBankCard().getBin(), ((PaymentToolDetailsBankCard) paymentToolDetails).getBin());
    }

}
