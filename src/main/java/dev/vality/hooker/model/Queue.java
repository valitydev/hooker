package dev.vality.hooker.model;

import dev.vality.hooker.retry.RetryPolicyRecord;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Queue {
    private long id;
    private Hook hook;
    private RetryPolicyRecord retryPolicyRecord;
}
