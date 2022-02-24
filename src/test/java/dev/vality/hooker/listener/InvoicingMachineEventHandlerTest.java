package dev.vality.hooker.listener;

import dev.vality.damsel.payment_processing.Event;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.exception.ParseException;
import dev.vality.hooker.handler.poller.invoicing.AbstractInvoiceEventMapper;
import dev.vality.hooker.service.BatchService;
import dev.vality.hooker.service.HandlerManager;
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

import static org.mockito.ArgumentMatchers.any;

public class InvoicingMachineEventHandlerTest {

    @Mock
    private HandlerManager handlerManager;
    @Mock
    private AbstractInvoiceEventMapper handler;
    @Mock
    private MachineEventParser<EventPayload> eventParser;
    @Mock
    private Acknowledgment ack;

    private InvoicingMachineEventHandler machineEventHandler;

    private BatchService batchService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        machineEventHandler = new InvoicingMachineEventHandler(handlerManager, eventParser, batchService);
    }

    @Test
    public void listenEmptyChanges() {
        Mockito.when(handlerManager.getHandler(any())).thenReturn(java.util.Optional.of(handler));

        MachineEvent message = new MachineEvent();
        Event event = new Event();
        EventPayload payload = new EventPayload();
        payload.setInvoiceChanges(new ArrayList<>());
        event.setPayload(payload);
        Mockito.when(eventParser.parse(message)).thenReturn(payload);

        machineEventHandler.handle(Collections.singletonList(message), ack);

        Mockito.verify(handlerManager, Mockito.times(0)).getHandler(any());
        Mockito.verify(handler, Mockito.times(0)).handle(any(), any(), any());
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
        Mockito.when(handlerManager.getHandler(any())).thenReturn(java.util.Optional.of(handler));

        machineEventHandler.handle(Collections.singletonList(message), ack);

        Mockito.verify(handlerManager, Mockito.times(1)).getHandler(any());
        Mockito.verify(handler, Mockito.times(1)).handle(any(), any(), any());
        Mockito.verify(ack, Mockito.times(1)).acknowledge();
    }

}