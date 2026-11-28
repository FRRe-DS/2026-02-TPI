package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.AvailabilityDeliveryPort;
import ar.edu.utn.frre.m3.application.port.M4AvailabilityPort;
import ar.edu.utn.frre.m3.domain.AvailabilityMessage;
import ar.edu.utn.frre.m3.domain.DeliveryResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * T7 · Qué hace el worker con cada respuesta de M4.
 *
 * <p>El reloj está fijo para poder afirmar el momento exacto del próximo intento.
 */
class DeliverAvailabilityServiceTest {

    static final Instant NOW = Instant.parse("2026-11-28T12:00:00Z");
    AvailabilityDeliveryPort queue;
    M4AvailabilityPort m4;
    DeliverAvailabilityService service;

    @BeforeEach
    void setUp() {
        queue = mock(AvailabilityDeliveryPort.class);
        m4 = mock(M4AvailabilityPort.class);
        service = new DeliverAvailabilityService(queue, m4, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    AvailabilityMessage attempt(int number) {
        return new AvailabilityMessage(UUID.randomUUID(), UUID.randomUUID(), 1, "{}", number);
    }

    void queueHolds(AvailabilityMessage message) {
        when(queue.claim(any(), any())).thenReturn(Optional.of(message), Optional.empty());
    }

    @Test
    @DisplayName("M4 confirma: el mensaje termina como aplicado")
    void confirmedIntentIsFinishedAsApplied() {
        var message = attempt(1);
        queueHolds(message);
        when(m4.send(message)).thenReturn(DeliveryResult.APPLIED);

        service.deliverBatch();

        verify(queue, atLeastOnce()).claim(NOW, NOW.plusSeconds(60)); // la segunda vuelta encuentra la cola vacía
        verify(queue).finish(message, true, false, NOW.plusSeconds(2), NOW);
    }

    @Test
    @DisplayName("M4 no responde: se reprograma con espera creciente y no se marca terminal")
    void unavailableM4IsRetriedWithBackoff() {
        var first = attempt(1);
        queueHolds(first);
        when(m4.send(first)).thenReturn(DeliveryResult.RETRY);
        service.deliverBatch();
        verify(queue).finish(first, false, false, NOW.plusSeconds(2), NOW);

        var fourth = attempt(4);
        queueHolds(fourth);
        when(m4.send(fourth)).thenReturn(DeliveryResult.RETRY);
        service.deliverBatch();
        verify(queue).finish(fourth, false, false, NOW.plusSeconds(16), NOW);
    }

    @Test
    @DisplayName("Una excepción inesperada del adaptador cuenta como reintento, no corta el lote")
    void unexpectedAdapterErrorIsTreatedAsRetry() {
        var message = attempt(2);
        queueHolds(message);
        when(m4.send(message)).thenThrow(new IllegalStateException("socket cerrado"));

        service.deliverBatch();

        verify(queue).finish(message, false, false, NOW.plusSeconds(4), NOW);
    }

    @Test
    @DisplayName("Sexto intento fallido: queda FAILED hasta un reintento manual")
    void sixthFailedAttemptIsTerminal() {
        var message = attempt(6);
        queueHolds(message);
        when(m4.send(message)).thenReturn(DeliveryResult.RETRY);

        service.deliverBatch();

        verify(queue).finish(message, false, true, NOW.plusSeconds(60), NOW);
    }

    @Test
    @DisplayName("M4 rechaza: terminal desde el primer intento")
    void rejectionIsTerminalImmediately() {
        var message = attempt(1);
        queueHolds(message);
        when(m4.send(message)).thenReturn(DeliveryResult.REJECTED);

        service.deliverBatch();

        verify(queue).finish(message, false, true, NOW.plusSeconds(2), NOW);
    }

    @Test
    @DisplayName("Cola vacía: no llama a M4")
    void emptyQueueDoesNotCallM4() {
        when(queue.claim(any(), any())).thenReturn(Optional.empty());

        service.deliverBatch();

        verifyNoInteractions(m4);
        verify(queue, never()).finish(any(), anyBoolean(), anyBoolean(), any(), any());
    }
}
