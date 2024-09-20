package dev.vality.hooker.dao.impl;

import dev.vality.hooker.dao.WebhookAdditionalFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class AllHookTablesRow {
    private long id;
    private String partyId;
    private String topic;
    private String url;
    private String pubKey;
    private boolean enabled;
    private double availability;
    private String createdAt;
    private WebhookAdditionalFilter webhookAdditionalFilter;

}
