package dev.vality.hooker.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.domain.*;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.swag_webhook_events.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.vality.swag_webhook_events.model.PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_DIGITAL_WALLET;
import static org.junit.jupiter.api.Assertions.*;

class PaymentToolUtilsTest {

    @Test
    void testGetPaymentToolDetailsCryptoWallet() {
        PaymentTool paymentTool = PaymentTool.crypto_currency(new CryptoCurrencyRef("bitcoin"));
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertInstanceOf(PaymentToolDetailsCryptoWallet.class, paymentToolDetails);
        assertEquals(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_CRYPTO_WALLET,
                paymentToolDetails.getDetailsType());
        assertEquals(dev.vality.swag_webhook_events.model.CryptoCurrency.BITCOIN.getValue(),
                ((PaymentToolDetailsCryptoWallet) paymentToolDetails).getCryptoCurrencyType());
    }

    @Test
    void testDigitalWalletJson() throws JsonProcessingException {
        PaymentTool paymentTool =
                PaymentTool.digital_wallet(new DigitalWallet("kke"));
        paymentTool.getDigitalWallet().setPaymentService(new PaymentServiceRef("qiwi"));
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        String json = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(paymentToolDetails);
        assertEquals(PAYMENT_TOOL_DETAILS_DIGITAL_WALLET, paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsDigitalWallet.DigitalWalletDetailsTypeEnum.DIGITAL_WALLET_DETAILS_QIWI,
                ((PaymentToolDetailsDigitalWallet) paymentToolDetails).getDigitalWalletDetailsType());
    }

    @Test
    void testGetPaymentToolDetailsPaymentTerminal() {
        PaymentTool paymentTool = PaymentTool.payment_terminal(new PaymentTerminal()
                .setPaymentService(new PaymentServiceRef("alipay"))
        );
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertInstanceOf(PaymentToolDetailsPaymentTerminal.class, paymentToolDetails);
        assertEquals(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_PAYMENT_TERMINAL,
                paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsPaymentTerminal.ProviderEnum.ALIPAY.getValue(),
                ((PaymentToolDetailsPaymentTerminal) paymentToolDetails).getProvider().getValue());

        paymentTool = PaymentTool.payment_terminal(new PaymentTerminal()
                .setPaymentService(new PaymentServiceRef("wechat"))
        );
        paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertInstanceOf(PaymentToolDetailsPaymentTerminal.class, paymentToolDetails);
        assertEquals(PaymentToolDetails.DetailsTypeEnum.PAYMENT_TOOL_DETAILS_PAYMENT_TERMINAL,
                paymentToolDetails.getDetailsType());
        assertEquals(PaymentToolDetailsPaymentTerminal.ProviderEnum.WECHAT.getValue(),
                ((PaymentToolDetailsPaymentTerminal) paymentToolDetails).getProvider().getValue());
    }

    @Test
    void testGetPaymentToolDetailsBankCard() throws IOException {
        PaymentTool paymentTool = PaymentTool.bank_card(new MockTBaseProcessor(MockMode.RANDOM, 15, 2)
                .process(new BankCard(), new TBaseHandler<>(BankCard.class)));
        PaymentToolDetails paymentToolDetails = PaymentToolUtils.getPaymentToolDetails(paymentTool);
        assertInstanceOf(PaymentToolDetailsBankCard.class, paymentToolDetails);
        assertNotNull(paymentToolDetails.getDetailsType());
        assertEquals(paymentTool.getBankCard().getBin(), ((PaymentToolDetailsBankCard) paymentToolDetails).getBin());
    }

}
