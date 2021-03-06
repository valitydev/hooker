package dev.vality.hooker.handler;

import dev.vality.geck.filter.Filter;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.Message;

public interface Mapper<C, M extends Message> {
    default boolean accept(C change) {
        return getFilter().match(change);
    }

    M map(C change, EventInfo eventInfo);

    Filter getFilter();
}