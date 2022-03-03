package dev.vality.hooker.dao;

import dev.vality.hooker.model.Message;
import dev.vality.hooker.model.WebhookMessageModel;

import java.util.List;

public interface MessageDao<M extends Message> {
    Long save(M message);

    List<WebhookMessageModel<M>> getWebhookModels(Long messageId);

    Long getParentEventId(Long hookId, String sourceId, Long messageId);
}