package dev.vality.hooker.converter;

import dev.vality.damsel.domain.DisposablePaymentResource;
import dev.vality.hooker.utils.ErrorUtils;
import dev.vality.hooker.utils.PaymentToolUtils;
import dev.vality.swag_webhook_events.model.ClientInfo;
import dev.vality.swag_webhook_events.model.CustomerBinding;
import dev.vality.swag_webhook_events.model.PaymentResource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerBindingConverter
        implements Converter<dev.vality.damsel.payment_processing.CustomerBinding, CustomerBinding> {

    @Override
    public CustomerBinding convert(dev.vality.damsel.payment_processing.CustomerBinding source) {
        DisposablePaymentResource paymentResource = source.getPaymentResource();
        return new CustomerBinding()
                .status(CustomerBinding.StatusEnum.fromValue(source.getStatus().getSetField().getFieldName()))
                .error(source.getStatus().isSetFailed()
                        ? ErrorUtils.getCustomerBindingError(source.getStatus().getFailed().getFailure()) : null)
                .id(source.getId())
                .paymentResource(new PaymentResource()
                        .paymentSession(paymentResource.getPaymentSessionId())
                        .clientInfo(new ClientInfo()
                                .ip(paymentResource.isSetClientInfo()
                                        ? paymentResource.getClientInfo().getIpAddress() : null)
                                .fingerprint(paymentResource.isSetClientInfo()
                                        ? paymentResource.getClientInfo().getFingerprint() : null))
                        .paymentToolDetails(PaymentToolUtils.getPaymentToolDetails(paymentResource.getPaymentTool())));
    }
}
