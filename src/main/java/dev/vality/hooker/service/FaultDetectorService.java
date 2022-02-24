package dev.vality.hooker.service;

public interface FaultDetectorService {

    double getRate(long hookId);

    void startRequest(long hookId, long eventId);

    void finishRequest(long hookId, long eventId);

    void errorRequest(long hookId, long eventId);

}
