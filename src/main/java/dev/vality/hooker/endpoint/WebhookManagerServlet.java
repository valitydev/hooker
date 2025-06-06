package dev.vality.hooker.endpoint;

import dev.vality.damsel.webhooker.WebhookManagerSrv;
import dev.vality.woody.api.event.CompositeServiceEventListener;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import dev.vality.woody.thrift.impl.http.event.HttpServiceEventLogListener;
import dev.vality.woody.thrift.impl.http.event.ServiceEventLogListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/hook")
@Slf4j
@RequiredArgsConstructor
public class WebhookManagerServlet extends GenericServlet {

    private final WebhookManagerSrv.Iface requestHandler;
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
                .build(WebhookManagerSrv.Iface.class, requestHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        log.info("Start new request to WebhookManagerServlet.");
        thriftServlet.service(req, res);
    }
}
