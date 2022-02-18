package dev.vality.hooker.model;

import lombok.Data;

@Data
public class WebhookMessageModel<T extends Message> {
    private T message;
    private Long hookId;
    private String url;
    private String privateKey;
}
