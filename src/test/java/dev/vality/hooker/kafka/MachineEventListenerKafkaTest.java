package dev.vality.hooker.kafka;

import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.hooker.config.KafkaTest;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.eventsink.SinkEvent;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import dev.vality.testcontainers.annotations.kafka.config.KafkaProducer;
import org.apache.thrift.TBase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;

@KafkaTest
@PostgresqlSpringBootITest
@SpringBootTest
@Testcontainers
class MachineEventListenerKafkaTest {

    public static final String SOURCE_ID = "source_id";
    public static final String SOURCE_NS = "source_ns";

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("kafka.ssl.enabled", () -> "false");
        registry.add("kafka.topics.invoice.enabled", () -> "true");
    }

    @Value("${kafka.topics.invoice.id}")
    private String invoiceTopic;

    @MockitoBean
    private MachineEventParser<EventPayload> eventParser;

    @Autowired
    private KafkaProducer<TBase<?, ?>> testThriftKafkaProducer;

    @Test
    void listenInvoiceEmptyChanges() {
        Mockito.when(eventParser.parse(any())).thenReturn(EventPayload.invoice_changes(emptyList()));

        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(createMessage());

        testThriftKafkaProducer.send(invoiceTopic, sinkEvent);

        Mockito.verify(eventParser, Mockito.timeout(10000L).times(1)).parse(any());
    }

    private MachineEvent createMessage() {
        MachineEvent message = new MachineEvent();
        var data = new dev.vality.machinegun.msgpack.Value();
        data.setBin(new byte[0]);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        message.setSourceNs(SOURCE_NS);
        message.setSourceId(SOURCE_ID);
        message.setData(data);
        return message;
    }
}
