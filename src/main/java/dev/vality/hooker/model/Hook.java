package dev.vality.hooker.model;

import dev.vality.hooker.dao.WebhookAdditionalFilter;
import lombok.*;

import java.util.Set;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Hook {
    private long id;
    private String partyId;
    private String topic;
    @ToString.Exclude
    private Set<WebhookAdditionalFilter> filters;
    private String url;
    @ToString.Exclude
    private String pubKey;
    @ToString.Exclude
    private String privKey;
    private boolean enabled;
    private double availability;
    private String createdAt;
}
