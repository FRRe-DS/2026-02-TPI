package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.application.location.LocationUpdate;

/** Output port used by the isolated location simulator to publish positions to M4. */
public interface M4LocationPort {
    void sendLocation(LocationUpdate update);
}
