package pe.utec.dbp.labreserve.model;

import java.util.Locale;
import java.util.Optional;

public enum ReservationStatus {
    RESERVED,
    USED,
    CANCELLED;

    /**
     * Traduce el query param {@code status} (all | reserved | used | cancelled).
     * Un {@code Optional} vacio significa "sin filtro".
     */
    public static Optional<ReservationStatus> parseFilter(String raw) {
        if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw.trim())) {
            return Optional.empty();
        }
        return Optional.of(ReservationStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
    }
}
