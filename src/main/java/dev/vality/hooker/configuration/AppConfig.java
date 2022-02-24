package dev.vality.hooker.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.vality.hooker.dao.CustomerDao;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.dao.impl.CustomerQueueDao;
import dev.vality.hooker.dao.impl.CustomerTaskDao;
import dev.vality.hooker.dao.impl.InvoicingQueueDao;
import dev.vality.hooker.dao.impl.InvoicingTaskDao;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerQueue;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingQueue;
import dev.vality.hooker.retry.RetryPoliciesService;
import dev.vality.hooker.scheduler.MessageScheduler;
import dev.vality.hooker.scheduler.MessageSender;
import dev.vality.hooker.service.CustomerEventService;
import dev.vality.hooker.service.FaultDetectorService;
import dev.vality.hooker.service.InvoicingEventService;
import dev.vality.hooker.service.MessageProcessor;
import dev.vality.hooker.service.crypt.Signer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;


@Configuration
public class AppConfig {

    @Value("${message.scheduler.invoicing.threadPoolSize}")
    private int invoicingThreadPoolSize;

    @Value("${message.scheduler.customer.threadPoolSize}")
    private int customerThreadPoolSize;

    @Value("${message.scheduler.delay}")
    private int delayMillis;

    @Value("${merchant.callback.timeout}")
    private int timeout;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public MessageSender<InvoicingMessage, InvoicingQueue> invoicngMessageSender(
            Signer signer, InvoicingEventService eventService,
            ObjectMapper objectMapper, FaultDetectorService faultDetector) {
        return new MessageSender<>(invoicingThreadPoolSize, timeout, signer, eventService, objectMapper, faultDetector);
    }

    @Bean
    public MessageSender<CustomerMessage, CustomerQueue> customerMessageSender(
            Signer signer, CustomerEventService eventService,
            ObjectMapper objectMapper, FaultDetectorService faultDetector) {
        return new MessageSender<>(customerThreadPoolSize, timeout, signer, eventService, objectMapper, faultDetector);
    }

    @Bean
    public MessageProcessor<InvoicingMessage, InvoicingQueue> invoicingMessageProcessor(
            HookDao hookDao, InvoicingTaskDao taskDao, InvoicingQueueDao queueDao, InvoicingMessageDao messageDao,
            RetryPoliciesService retryPoliciesService, TransactionTemplate transactionTemplate,
            FaultDetectorService faultDetector, MessageSender<InvoicingMessage, InvoicingQueue> invoicngMessageSender) {
        return new MessageProcessor<>(hookDao, taskDao, queueDao, messageDao, retryPoliciesService, transactionTemplate,
                faultDetector, invoicngMessageSender);
    }

    @Bean
    public MessageProcessor<CustomerMessage, CustomerQueue> customerMessageProcessor(
            HookDao hookDao, CustomerTaskDao taskDao, CustomerQueueDao queueDao, CustomerDao messageDao,
            RetryPoliciesService retryPoliciesService, TransactionTemplate transactionTemplate,
            FaultDetectorService faultDetector, MessageSender<CustomerMessage, CustomerQueue> customerMessageSender) {
        return new MessageProcessor<>(hookDao, taskDao, queueDao, messageDao, retryPoliciesService, transactionTemplate,
                faultDetector, customerMessageSender);
    }

    @Bean
    public MessageScheduler<InvoicingMessage, InvoicingQueue> invoicingMessageScheduler(
            MessageProcessor<InvoicingMessage, InvoicingQueue> invoicingMessageProcessor,
            ThreadPoolTaskScheduler taskScheduler) {
        return new MessageScheduler<>(invoicingThreadPoolSize, delayMillis, invoicingMessageProcessor, taskScheduler);
    }

    @Bean
    public MessageScheduler<CustomerMessage, CustomerQueue> cuustomerMessageScheduler(
            MessageProcessor<CustomerMessage, CustomerQueue> customerMessageProcessor,
            ThreadPoolTaskScheduler taskScheduler) {
        return new MessageScheduler<>(customerThreadPoolSize, delayMillis, customerMessageProcessor, taskScheduler);
    }
}
