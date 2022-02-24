package dev.vality.hooker.listener;

import dev.vality.machinegun.eventsink.MachineEvent;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface MachineEventHandler {

    void handle(List<MachineEvent> messages, Acknowledgment ack);

}
