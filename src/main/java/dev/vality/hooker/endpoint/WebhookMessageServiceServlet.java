package dev.vality.hooker.endpoint;

import dev.vality.damsel.webhooker.WebhookMessageServiceSrv;
import dev.vality.woody.api.event.CompositeServiceEventListener;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import dev.vality.woody.thrift.impl.http.event.HttpServiceEventLogListener;
import dev.vality.woody.thrift.impl.http.event.ServiceEventLogListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;

import java.io.IOException;

@WebServlet("/message")
@Slf4j
@RequiredArgsConstructor
public class WebhookMessageServiceServlet extends GenericServlet {

    private final WebhookMessageServiceSrv.Iface requestHandler;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("Hooker servlet init.");
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .withEventListener(
                        new CompositeServiceEventListener<>(
                                new ServiceEventLogListener(),
                                new HttpServiceEventLogListener()
                        )
                )
                .build(WebhookMessageServiceSrv.Iface.class, requestHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        log.info("Start new request to WebhookMessageServiceServlet.");
        thriftServlet.service(req, res);
    }
}
