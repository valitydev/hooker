package dev.vality.hooker.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Message {
    private Long id;
    private Long sequenceId;
    private Integer changeId;
    private String eventTime;
    private String sourceId;
    private String partyId;
    private String shopId;
    private EventType eventType;
}
