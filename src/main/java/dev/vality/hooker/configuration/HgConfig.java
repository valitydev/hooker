package dev.vality.hooker.configuration;

import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class HgConfig {
    @Bean
    public InvoicingSrv.Iface invoicingClient(@Value("${service.invoicing.url}") Resource resource,
                                              @Value("${service.invoicing.networkTimeout}") int networkTimeout)
            throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(InvoicingSrv.Iface.class);
    }
}
