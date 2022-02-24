package dev.vality.hooker.service;

import dev.vality.hooker.model.Message;
import dev.vality.swag_webhook_events.model.Event;

public interface EventService<M extends Message> {

    Event getEventByMessage(M message);

}
