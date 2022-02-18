package dev.vality.hooker.model;

import dev.vality.hooker.dao.WebhookAdditionalFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hook {
    private long id;
    private String partyId;
    private String topic;
    private Set<WebhookAdditionalFilter> filters;
    private String url;
    private String pubKey;
    private String privKey;
    private boolean enabled;
    private double availability;

    @Override
    public String toString() {
        return "Hook{" +
                "id=" + id +
                ", topic=" + topic +
                ", partyId='" + partyId + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
