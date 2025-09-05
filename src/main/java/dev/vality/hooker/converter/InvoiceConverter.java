package dev.vality.hooker.converter;

import dev.vality.damsel.domain.InvoiceLine;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.Invoice;
import dev.vality.swag_webhook_events.model.InvoiceCartLine;
import dev.vality.swag_webhook_events.model.InvoiceCartLineTaxMode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvoiceConverter implements Converter<dev.vality.damsel.domain.Invoice, Invoice> {

    private final MetadataDeserializer deserializer;

    @Override
    public Invoice convert(dev.vality.damsel.domain.Invoice source) {
        return new Invoice()
                .id(source.getId())
                .shopID(source.getShopRef().getId())
                .createdAt(TimeUtils.toOffsetDateTime(source.getCreatedAt()))
                .status(Invoice.StatusEnum.fromValue(source.getStatus().getSetField().getFieldName()))
                .dueDate(TimeUtils.toOffsetDateTime(source.getDue()))
                .amount(source.getCost().getAmount())
                .currency(source.getCost().getCurrency().getSymbolicCode())
                .metadata(source.isSetContext() ? deserializer.deserialize(source.getContext().getData()) : null)
                .product(source.getDetails().getProduct())
                .reason(source.getStatus().isSetCancelled() ? source.getStatus().getCancelled().getDetails() :
                        source.getStatus().isSetFulfilled() ? source.getStatus().getFulfilled().getDetails() : null)
                .description(source.getDetails().getDescription())
                .cart(source.getDetails().isSetCart() ? convertCart(source.getDetails().getCart().getLines()) : null)
                .externalId(source.getExternalId());
    }

    private List<InvoiceCartLine> convertCart(List<InvoiceLine> cartLines) {
        return cartLines.stream().map(l ->
                new InvoiceCartLine()
                        .product(l.getProduct())
                        .price(l.getPrice().getAmount())
                        .quantity((long) l.getQuantity())
                        .cost(l.getPrice().getAmount() * l.getQuantity())
                        .taxMode(l.getMetadata() != null && l.getMetadata().get("TaxMode") != null
                                ? new InvoiceCartLineTaxMode()
                                .rate(InvoiceCartLineTaxMode.RateEnum
                                        .fromValue(l.getMetadata().get("TaxMode").getStr()))
                                : null))
                .collect(Collectors.toList());
    }
}
