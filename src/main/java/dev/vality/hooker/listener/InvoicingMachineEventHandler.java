package dev.vality.hooker.listener;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.service.HandlerManager;
import dev.vality.hooker.service.MessageService;
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

    private final HandlerManager handlerManager;
    private final MachineEventParser<EventPayload> parser;
    private final MessageService<InvoicingMessage> invoicingMessageService;

    @Override
    @Transactional
    public void handle(List<MachineEvent> machineEvents, Acknowledgment ack) {
        machineEvents.forEach(me -> {
            EventPayload payload = parser.parse(me);
            if (payload.isSetInvoiceChanges()) {
                for (int i = 0; i < payload.getInvoiceChanges().size(); ++i) {
                    InvoiceChange invoiceChange = payload.getInvoiceChanges().get(i);
                    int j = i;
                    handlerManager.getHandler(invoiceChange).ifPresent(handler -> {
                        log.info("Start to handle event {}", invoiceChange);
                        EventInfo eventInfo = new EventInfo(me.getCreatedAt(), me.getSourceId(), me.getEventId(), j);
                        InvoicingMessage message = handler.map(invoiceChange, eventInfo);
                        if (message != null) {
                            invoicingMessageService.process(message);
                        }
                    });
                }
            }
        });
        ack.acknowledge();
    }
}
