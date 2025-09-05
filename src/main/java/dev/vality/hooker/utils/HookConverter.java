package dev.vality.hooker.utils;

import dev.vality.damsel.domain.PartyConfigRef;
import dev.vality.damsel.webhooker.Webhook;
import dev.vality.damsel.webhooker.WebhookParams;
import dev.vality.hooker.model.Hook;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jeckep on 13.04.17.
 */
public class HookConverter {
    public static Webhook convert(Hook hook) {
        return new Webhook(
                hook.getId(),
                new PartyConfigRef(hook.getPartyId()),
                EventFilterUtils.getEventFilter(hook.getFilters()),
                hook.getUrl(),
                buildFormattedPubKey(hook.getPubKey()),
                hook.isEnabled());
    }

    public static Hook convert(WebhookParams webhookParams) {
        Hook hook = new Hook();
        hook.setPartyId(webhookParams.getPartyRef().getId());
        hook.setTopic(EventFilterUtils.getTopic(webhookParams.getEventFilter()));
        hook.setUrl(webhookParams.getUrl());
        hook.setFilters(EventFilterUtils.getWebhookAdditionalFilter(webhookParams.getEventFilter()));

        return hook;
    }

    public static List<Webhook> convert(List<Hook> hooks) {
        return hooks.stream().map(h -> convert(h)).collect(Collectors.toList());
    }

    private static String buildFormattedPubKey(String key) {
        return "-----BEGIN PUBLIC KEY-----\n" +
                key.replaceAll("(.{64})", "$1\n") +
                "\n-----END PUBLIC KEY-----";
    }
}
