package dev.vality.hooker.model;

import dev.vality.hooker.dao.WebhookAdditionalFilter;
import dev.vality.hooker.retry.RetryPolicyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Created by jeckep on 13.04.17.
 */
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
    private RetryPolicyType retryPolicyType;

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
