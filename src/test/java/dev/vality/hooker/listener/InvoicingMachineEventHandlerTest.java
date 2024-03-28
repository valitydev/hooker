package dev.vality.hooker.listener;

import dev.vality.damsel.payment_processing.Event;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.exception.ParseException;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.service.InvoiceMessageService;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

public class InvoicingMachineEventHandlerTest {

    @Mock
    private Mapper<InvoiceChange, InvoicingMessage> handler;
    @Mock
    private MachineEventParser<EventPayload> eventParser;
    @Mock
    private Acknowledgment ack;

    private InvoicingMachineEventHandler machineEventHandler;

    private InvoiceMessageService invoicingService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        machineEventHandler = new InvoicingMachineEventHandler(List.of(handler), eventParser, invoicingService);
    }

    @Test
    public void listenEmptyChanges() {
        MachineEvent message = new MachineEvent();
        Event event = new Event();
        EventPayload payload = new EventPayload();
        payload.setInvoiceChanges(new ArrayList<>());
        event.setPayload(payload);
        Mockito.when(eventParser.parse(message)).thenReturn(payload);

        machineEventHandler.handle(Collections.singletonList(message), ack);

        Mockito.verify(handler, Mockito.times(0)).map(any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

    @Test(expected = ParseException.class)
    public void listenEmptyException() {
        MachineEvent message = new MachineEvent();
        Mockito.when(eventParser.parse(message)).thenThrow(new ParseException());
        machineEventHandler.handle(Collections.singletonList(message), ack);

        Mockito.verify(ack, Mockito.times(0)).acknowledge();
    }

    @Test
    public void listenChanges() {
        ArrayList<InvoiceChange> invoiceChanges = new ArrayList<>();
        invoiceChanges.add(new InvoiceChange());
        EventPayload payload = new EventPayload();
        payload.setInvoiceChanges(invoiceChanges);
        Event event = new Event();
        event.setPayload(payload);
        MachineEvent message = new MachineEvent();

        Mockito.when(eventParser.parse(message)).thenReturn(payload);

        machineEventHandler.handle(Collections.singletonList(message), ack);

        Mockito.verify(handler, Mockito.times(1)).accept(any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

}
