package dev.vality.hooker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventInfo {
    private String eventCreatedAt;
    private String sourceId;
    private Long sequenceId;
    private Integer changeId;
}
