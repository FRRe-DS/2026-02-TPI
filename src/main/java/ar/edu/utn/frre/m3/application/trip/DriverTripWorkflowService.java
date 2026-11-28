package ar.edu.utn.frre.m3.application.trip;

import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.location.GeoPosition;
import ar.edu.utn.frre.m3.application.location.LocationSimulationService;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.M5OfferPort;
import ar.edu.utn.frre.m3.application.port.M6TripLifecyclePort;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.IntegrationUnavailableException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Coordinates M5/M6 requests and the small amount of trip state M3 needs for its driver UI. */
public class DriverTripWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(DriverTripWorkflowService.class);
    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final DriverRepository drivers;
    private final AvailabilityIntentRepository availability;
    private final M5OfferPort m5;
    private final M6TripLifecyclePort m6;
    private final LocationSimulationService locations;
    private final Clock clock;
    private final Map<UUID, DriverOffer> offers = new ConcurrentHashMap<>();
    private final Map<UUID, DriverTrip> trips = new ConcurrentHashMap<>();
    private final Map<String, DriverTrip> completedEvents = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> ratings = new ConcurrentHashMap<>();

    public DriverTripWorkflowService(DriverRepository drivers, AvailabilityIntentRepository availability,
            M5OfferPort m5, M6TripLifecyclePort m6, LocationSimulationService locations, Clock clock) {
        this.drivers = drivers;
        this.availability = availability;
        this.m5 = m5;
        this.m6 = m6;
        this.locations = locations;
        this.clock = clock;
    }

    public DriverOffer receiveOffer(DriverOffer incoming) {
        drivers.findById(incoming.driverId()).orElseThrow(() -> DriverNotFoundException.forId(incoming.driverId()));
        DriverOffer existing = offers.get(incoming.offerId());
        if (existing != null) {
            if (!sameOffer(existing, incoming)) {
                throw new DomainValidationException("M5 reutilizó el identificador de una oferta con datos diferentes.");
            }
            return existing;
        }
        AvailabilityIntent intent = availability.findByDriverId(incoming.driverId())
                .orElseThrow(() -> new DomainValidationException("El conductor no tiene una intención de disponibilidad activa."));
        if (!intent.isConnected() || !intent.isAvailable()) {
            throw new DomainValidationException("El conductor no está conectado y disponible para recibir ofertas.");
        }
        if (incoming.expiresAt() == null || !incoming.expiresAt().isAfter(now())) {
            throw new DomainValidationException("La oferta de M5 ya venció.");
        }
        DriverTrip active = trips.get(incoming.driverId());
        if (active != null && active.isActive()) {
            throw new DomainValidationException("El conductor ya tiene un viaje asignado.");
        }
        DriverOffer raced = offers.putIfAbsent(incoming.offerId(), incoming);
        if (raced != null && !sameOffer(raced, incoming)) {
            throw new DomainValidationException("M5 reutilizó el identificador de una oferta con datos diferentes.");
        }
        if (raced == null) log.info("[TRIP] driver={} received offer={}", incoming.driverId(), incoming.offerId());
        return raced == null ? incoming : raced;
    }

    public List<DriverOffer> offersFor(UUID driverId) {
        return offers.values().stream().filter(o -> o.driverId().equals(driverId)
                        && o.status() == DriverOffer.OfferStatus.PENDING && o.expiresAt().isAfter(now()))
                .sorted(Comparator.comparing(DriverOffer::expiresAt)).toList();
    }

    public synchronized DriverOffer respond(UUID driverId, UUID offerId, String decision) {
        DriverOffer offer = offers.get(offerId);
        if (offer == null || !offer.driverId().equals(driverId)) {
            throw new DomainValidationException("No se encontró esa oferta para el conductor actual.");
        }
        if (offer.status() != DriverOffer.OfferStatus.PENDING || !offer.expiresAt().isAfter(now())) {
            throw new DomainValidationException("La oferta ya fue respondida o venció.");
        }
        if (!"ACCEPTED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException("La respuesta debe ser ACCEPTED o REJECTED.");
        }
        if ("ACCEPTED".equals(decision)) {
            AvailabilityIntent intent = availability.findByDriverId(driverId).orElse(null);
            if (intent == null || !intent.isConnected() || !intent.isAvailable()) {
                throw new DomainValidationException("Conectate y activá la disponibilidad antes de aceptar el pedido.");
            }
            DriverTrip active = trips.get(driverId);
            if (active != null && active.isActive()) {
                throw new DomainValidationException("El conductor ya tiene un viaje en curso.");
            }
        }
        OfferDecisionReceipt receipt;
        try {
            receipt = m5.respondToOffer(offerId, driverId, decision);
        } catch (OfferUnavailableException closed) {
            // M5 ya no la acepta (otra respuesta ganó, venció o se canceló): deja de mostrarse como pendiente.
            offers.put(offerId, offer.withDecision(DriverOffer.OfferStatus.EXPIRED, null));
            log.info("[TRIP] driver={} offer={} closed by M5: {}", driverId, offerId, closed.getMessage());
            throw closed;
        }
        if (receipt == null || !decision.equals(receipt.status())) {
            throw new IntegrationUnavailableException("M5 no confirmó la respuesta a la oferta.");
        }
        if ("ACCEPTED".equals(decision)) {
            if (receipt.tripId() == null || receipt.tripId().isBlank()) {
                throw new IntegrationUnavailableException("M5 aceptó la oferta sin informar el viaje asignado.");
            }
            DriverOffer accepted = offer.withDecision(DriverOffer.OfferStatus.ACCEPTED, receipt.tripId());
            offers.put(offerId, accepted);
            trips.put(driverId, new DriverTrip(receipt.tripId(), accepted, DriverTrip.TripStatus.ASSIGNED,
                    null, null, null, null));
            locations.setOccupied(driverId, true);
            log.info("[TRIP] driver={} accepted offer={} trip={}", driverId, offerId, receipt.tripId());
            return accepted;
        }
        DriverOffer rejected = offer.withDecision(DriverOffer.OfferStatus.REJECTED, null);
        offers.put(offerId, rejected);
        log.info("[TRIP] driver={} rejected offer={}", driverId, offerId);
        return rejected;
    }

    public synchronized DriverTrip start(UUID driverId, String tripId) {
        DriverTrip trip = requireTrip(driverId, tripId);
        if (trip.status() != DriverTrip.TripStatus.ASSIGNED) {
            throw new DomainValidationException("El viaje no está esperando el inicio.");
        }
        m6.startTrip(tripId, driverId);
        DriverTrip started = trip.withStatus(DriverTrip.TripStatus.IN_PROGRESS, now());
        trips.put(driverId, started);
        log.info("[TRIP] driver={} trip={} started", driverId, tripId);
        return started;
    }

    public synchronized void requestCompletion(UUID driverId, String tripId) {
        DriverTrip trip = requireTrip(driverId, tripId);
        if (trip.status() != DriverTrip.TripStatus.IN_PROGRESS) {
            throw new DomainValidationException("Sólo se puede finalizar un viaje que está en curso.");
        }
        m6.requestTripCompletion(tripId, driverId);
        log.info("[TRIP] driver={} trip={} completion requested from M6", driverId, tripId);
    }

    /** Applies M6's final event once, moves to the destination, and sends that point to M4. */
    public DriverTrip completedByM6(String tripId, UUID driverId, GeoPosition destination,
            Integer driverRating, OffsetDateTime completedAt) {
        if (driverRating != null && (driverRating < 1 || driverRating > 5)) {
            throw new IllegalArgumentException("La puntuación de M6 debe estar entre 1 y 5.");
        }
        String eventKey = driverId + ":" + tripId;
        return completedEvents.compute(eventKey, (ignored, previous) -> {
            if (previous != null) {
                if (!java.util.Objects.equals(previous.destination(), destination)
                        || !java.util.Objects.equals(previous.driverRating(), driverRating)
                        || (completedAt != null && !java.util.Objects.equals(previous.completedAt(), completedAt))) {
                    throw new DomainValidationException("M6 reenvió la finalización con datos diferentes.");
                }
                return previous;
            }
            DriverTrip trip = requireTrip(driverId, tripId);
            if (trip.status() != DriverTrip.TripStatus.IN_PROGRESS) {
                throw new DomainValidationException("M6 informó un viaje que M3 no tiene en curso.");
            }
            locations.setCompletedTripDestination(driverId, "FINALIZADO", destination);
            DriverTrip completed = trip.completed(completedAt == null ? now() : completedAt, destination, driverRating);
            trips.put(driverId, completed);
            if (driverRating != null) {
                ratings.computeIfAbsent(driverId, key -> new ConcurrentHashMap<>()).putIfAbsent(tripId, driverRating);
            }
            log.info("[TRIP] driver={} trip={} finalized by M6", driverId, tripId);
            return completed;
        });
    }

    /**
     * Alinea los viajes activos de M3 con el estado que tiene M6 (dueño del ciclo de vida).
     *
     * <p>M6 no le avisa a M3 por HTTP: publica {@code TripCompleted}/{@code TripCancelled} como eventos y
     * permite que un servicio lea {@code GET /api/v1/trips/{tripId}}. Mientras no haya un acuerdo de
     * mensajería, M3 consulta periódicamente cada viaje activo. Si M6 lo completó o lo canceló, libera
     * al conductor (deja de estar OCUPADO) y, si M6 informa el punto final, lo reubica allí.
     * Las llamadas a M6 se hacen fuera del bloqueo; el cambio se aplica sólo si el viaje sigue igual.
     *
     * @return cantidad de viajes que cambiaron de estado en esta pasada
     */
    public int syncWithM6() {
        List<DriverTrip> active = trips.values().stream().filter(DriverTrip::isActive).toList();
        int changed = 0;
        for (DriverTrip trip : active) {
            UUID driverId = trip.offer().driverId();
            M6TripState remote;
            try {
                remote = m6.findTrip(trip.tripId()).orElse(null);
            } catch (IntegrationUnavailableException unavailable) {
                log.warn("[TRIP] M6 no disponible al sincronizar trip={}: se reintenta en la próxima pasada", trip.tripId());
                continue;
            }
            if (remote == null) continue;
            if (remote.driverId() != null && !remote.driverId().equals(driverId)) {
                log.warn("[TRIP] M6 asigna trip={} a otro conductor; no se modifica", trip.tripId());
                continue;
            }
            if (applyRemoteState(driverId, trip, remote)) changed++;
        }
        return changed;
    }

    private synchronized boolean applyRemoteState(UUID driverId, DriverTrip seen, M6TripState remote) {
        if (trips.get(driverId) != seen) return false; // otro cambio ganó mientras se consultaba a M6
        if (remote.isCompleted()) {
            GeoPosition end = remote.finalLatitude() != null && remote.finalLongitude() != null
                    ? new GeoPosition(remote.finalLatitude(), remote.finalLongitude())
                    : remote.destinationLatitude() != null && remote.destinationLongitude() != null
                        ? new GeoPosition(remote.destinationLatitude(), remote.destinationLongitude()) : null;
            trips.put(driverId, seen.completed(remote.completedAt() == null ? now() : remote.completedAt(), end, null));
            locations.setOccupied(driverId, false);
            if (end != null) {
                try {
                    locations.setCompletedTripDestination(driverId, "FINALIZADO", end);
                } catch (RuntimeException positionNotPublished) {
                    log.warn("[TRIP] driver={} liberado, pero no se pudo publicar el punto final: {}", driverId, positionNotPublished.getMessage());
                }
            }
            log.info("[TRIP] driver={} trip={} completed in M6", driverId, seen.tripId());
            return true;
        }
        if (remote.isCancelled()) {
            locations.setOccupied(driverId, false);
            trips.put(driverId, seen.cancelled(now()));
            log.info("[TRIP] driver={} trip={} cancelled in M6", driverId, seen.tripId());
            return true;
        }
        if (remote.isInProgress() && seen.status() == DriverTrip.TripStatus.ASSIGNED) {
            trips.put(driverId, seen.withStatus(DriverTrip.TripStatus.IN_PROGRESS, now()));
            return true;
        }
        return false;
    }

    public DriverTrip currentTrip(UUID driverId) {
        return trips.get(driverId);
    }

    public RatingSummary ratingSummary(UUID driverId) {
        Map<String, Integer> driverRatings = ratings.getOrDefault(driverId, Map.of());
        if (driverRatings.isEmpty()) return new RatingSummary(0, 0);
        double average = driverRatings.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        return new RatingSummary(average, driverRatings.size());
    }

    private DriverTrip requireTrip(UUID driverId, String tripId) {
        DriverTrip trip = trips.get(driverId);
        if (trip == null || !trip.tripId().equals(tripId)) {
            throw new DomainValidationException("No se encontró ese viaje para el conductor actual.");
        }
        return trip;
    }

    private boolean sameOffer(DriverOffer first, DriverOffer second) {
        return first.offerId().equals(second.offerId()) && first.driverId().equals(second.driverId())
                && java.util.Objects.equals(first.requestId(), second.requestId())
                && java.util.Objects.equals(first.pickupAddress(), second.pickupAddress())
                && java.util.Objects.equals(first.destinationAddress(), second.destinationAddress())
                && java.util.Objects.equals(first.vehicleType(), second.vehicleType())
                && java.util.Objects.equals(first.expiresAt(), second.expiresAt());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ARGENTINA);
    }

    public record RatingSummary(double average, int count) {}
}
