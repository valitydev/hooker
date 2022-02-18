package dev.vality.hooker.service;

import dev.vality.damsel.webhooker.LimitExceeded;
import dev.vality.damsel.webhooker.Webhook;
import dev.vality.damsel.webhooker.WebhookManagerSrv;
import dev.vality.damsel.webhooker.WebhookNotFound;
import dev.vality.damsel.webhooker.WebhookParams;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.model.Hook;
import dev.vality.hooker.utils.HookConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by inalarsanukaev on 06.04.17.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookManager implements WebhookManagerSrv.Iface {

    private final HookDao hookDao;
    private final HooksLimitService hooksLimitService;

    @Override
    public List<Webhook> getList(String s) throws TException {
        return HookConverter.convert(hookDao.getPartyHooks(s));
    }

    @Override
    public Webhook get(long id) throws WebhookNotFound {
        Hook hook = hookDao.getHookById(id);
        if (hook == null) {
            log.warn("Webhook not found: {}", id);
            throw new WebhookNotFound();
        }
        return HookConverter.convert(hook);
    }

    @Override
    public Webhook create(WebhookParams webhookParams) throws TException {
        if (hooksLimitService.isLimitExceeded(webhookParams)) {
            log.info("Hooks limit exceeded for webhookParams={}", webhookParams);
            throw new LimitExceeded();
        }
        Hook hook = hookDao.create(HookConverter.convert(webhookParams));
        log.info("Webhook created: {}", hook);
        return HookConverter.convert(hook);
    }

    @Override
    public void delete(long id) throws WebhookNotFound {
        try {
            hookDao.delete(id);
            log.info("Webhook deleted: {}", id);
        } catch (Exception e) {
            log.error("Fail to delete webhook: {}", id, e);
            throw new WebhookNotFound();
        }
    }
}
