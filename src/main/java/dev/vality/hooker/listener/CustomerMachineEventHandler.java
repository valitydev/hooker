package dev.vality.hooker.listener;

import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.EventInfo;
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
public class CustomerMachineEventHandler implements MachineEventHandler {

    private final MachineEventParser<EventPayload> parser;
    private final List<Mapper<CustomerChange, CustomerMessage>> customerEventMappers;
    private final MessageService<CustomerMessage> customerMessageService;

    @Override
    @Transactional
    public void handle(List<MachineEvent> machineEvents, Acknowledgment ack) {
        machineEvents.forEach(me -> {
            EventPayload payload = parser.parse(me);
            if (payload.isSetCustomerChanges()) {
                for (int i = 0; i < payload.getCustomerChanges().size(); ++i) {
                    CustomerChange customerChange = payload.getCustomerChanges().get(i);
                    int j = i;
                    customerEventMappers.stream()
                            .filter(handler -> handler.accept(customerChange))
                            .findFirst()
                            .ifPresent(handler -> {
                                log.info("Start to handle event {}", customerChange);
                                var eventInfo = new EventInfo(me.getCreatedAt(), me.getSourceId(), me.getEventId(), j);
                                CustomerMessage message = handler.map(customerChange, eventInfo);
                                if (message != null) {
                                    customerMessageService.process(message);
                                }
                            });
                }
            }
        });
        ack.acknowledge();
    }
}
