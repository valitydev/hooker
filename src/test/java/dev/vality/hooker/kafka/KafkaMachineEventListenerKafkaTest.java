package dev.vality.hooker.kafka;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.hooker.AbstractKafkaIntegrationTest;
import dev.vality.hooker.listener.CustomerEventKafkaListener;
import dev.vality.hooker.listener.CustomerMachineEventHandler;
import dev.vality.hooker.listener.InvoicingEventKafkaListener;
import dev.vality.hooker.listener.InvoicingMachineEventHandler;
import dev.vality.hooker.service.HandlerManager;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.thrift.TBase;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;

@Slf4j
@ContextConfiguration(classes = {
        KafkaAutoConfiguration.class,
        InvoicingEventKafkaListener.class,
        InvoicingMachineEventHandler.class,
        CustomerEventKafkaListener.class,
        CustomerMachineEventHandler.class
})
public class KafkaMachineEventListenerKafkaTest extends AbstractKafkaIntegrationTest {

    @org.springframework.beans.factory.annotation.Value("${kafka.topics.invoice.id}")
    private String invoiceTopic;

    @org.springframework.beans.factory.annotation.Value("${kafka.topics.customer.id}")
    private String customerTopic;

    @MockBean
    private HandlerManager handlerManager;

    @MockBean
    private MachineEventParser<EventPayload> eventParser;

    public static Producer<String, SinkEvent> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "client_id");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, new ThriftSerializer<SinkEvent>().getClass());
        return new KafkaProducer<>(props);
    }

    @Test
    public void listenInvoiceEmptyChanges() {
        Mockito.when(eventParser.parse(any())).thenReturn(EventPayload.invoice_changes(emptyList()));

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createMessage());

        writeToTopic(sinkEvent, invoiceTopic);

        Mockito.verify(eventParser, Mockito.timeout(10000L).times(1)).parse(any());
    }

    @Test
    public void listenCustomerEmptyChanges() {
        Mockito.when(eventParser.parse(any())).thenReturn(EventPayload.customer_changes(emptyList()));

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createMessage());

        writeToTopic(sinkEvent, customerTopic);

        Mockito.verify(eventParser, Mockito.timeout(10000L).times(1)).parse(any());
    }

    private void writeToTopic(SinkEvent sinkEvent, String topicName) {
        Producer<String, SinkEvent> producer = createProducer();
        ProducerRecord<String, SinkEvent> producerRecord = new ProducerRecord<>(topicName, null, sinkEvent);
        try {
            producer.send(producerRecord).get();
        } catch (Exception e) {
            log.error("KafkaAbstractTest initialize e: ", e);
        }
        producer.close();
    }

    private MachineEvent createMessage() {
        MachineEvent message = new MachineEvent();
        Value data = new Value();
        data.setBin(new byte[0]);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        message.setSourceNs(SOURCE_NS);
        message.setSourceId(SOURCE_ID);
        message.setData(data);
        return message;
    }
}
