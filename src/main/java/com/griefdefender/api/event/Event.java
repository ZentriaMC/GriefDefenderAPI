package com.griefdefender.api.event;

import com.griefdefender.api.GriefDefender;

public interface Event {

    EventCause getCause();

    default <T extends Event> T post() {
        GriefDefender.getEventManager().post(this);
        return (T) this;
    }
}
