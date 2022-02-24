package dev.vality.hooker.handler;

import dev.vality.geck.filter.Filter;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.Message;

/**
 * Created by inal on 24.11.2016.
 */
public interface Handler<C, M extends Message> {
    default boolean accept(C change) {
        return getFilter().match(change);
    }

    void handle(C change, EventInfo eventInfo);

    Filter getFilter();
}
