package dev.vality.hooker.listener;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.service.CustomerMessageService;
import dev.vality.hooker.service.InvoiceMessageService;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicingMachineEventHandler implements MachineEventHandler {

    private final List<Mapper<InvoiceChange, InvoicingMessage>> handlers;
    private final MachineEventParser<EventPayload> parser;
    private final InvoiceMessageService invoicingMessageService;

    @Override
    @Transactional
    public void handle(List<MachineEvent> machineEvents, Acknowledgment ack) {
        machineEvents.forEach(me -> {
            EventPayload payload = parser.parse(me);
            if (payload.isSetInvoiceChanges()) {
                handleChanges(me, payload);
            }
        });
        ack.acknowledge();
    }

    private void handleChanges(MachineEvent me, EventPayload payload) {
        for (int i = 0; i < payload.getInvoiceChanges().size(); ++i) {
            InvoiceChange invoiceChange = payload.getInvoiceChanges().get(i);
            handleChange(me, invoiceChange, i);
        }
    }

    private void handleChange(MachineEvent me, InvoiceChange invoiceChange, int i) {
        handlers.stream()
                .filter(handler -> handler.accept(invoiceChange))
                .findFirst()
                .ifPresent(handler -> {
                    log.info("Start to handle event {}", invoiceChange);
                    var eventInfo = new EventInfo(me.getCreatedAt(), me.getSourceId(), me.getEventId(), i);
                    InvoicingMessage message = handler.map(invoiceChange, eventInfo);
                    if (message != null) {
                        invoicingMessageService.process(message);
                    }
                });
    }
}
