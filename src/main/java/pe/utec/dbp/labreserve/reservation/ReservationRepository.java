package pe.utec.dbp.labreserve.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utec.dbp.labreserve.model.LabReservation;
import pe.utec.dbp.labreserve.model.ReservationStatus;

import java.time.ZonedDateTime;

public interface ReservationRepository extends JpaRepository<LabReservation, Long> {

    @Query("""
            SELECT COUNT(r) > 0 FROM LabReservation r
            WHERE r.slot.id = :slotId AND r.student.id = :studentId
            """)
    boolean existsBySlotAndStudent(@Param("slotId") Long slotId, @Param("studentId") Long studentId);

    @Query("""
            SELECT COUNT(r) FROM LabReservation r
            WHERE r.slot.id = :slotId AND r.status = :status
            """)
    long countActiveBySlot(@Param("slotId") Long slotId, @Param("status") ReservationStatus status);

    /**
     * Reservas vigentes del estudiante cuyo turno cruza el intervalo indicado.
     */
    @Query("""
            SELECT COUNT(r) FROM LabReservation r
            WHERE r.student.id = :studentId
              AND r.status = :status
              AND r.slot.startTime < :endTime
              AND r.slot.endTime > :startTime
            """)
    long countOverlapping(@Param("studentId") Long studentId,
                          @Param("startTime") ZonedDateTime startTime,
                          @Param("endTime") ZonedDateTime endTime,
                          @Param("status") ReservationStatus status);

    @Query(value = """
            SELECT r FROM LabReservation r
            JOIN FETCH r.slot
            WHERE r.student.id = :studentId
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.reservedAt DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM LabReservation r
            WHERE r.student.id = :studentId
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<LabReservation> findOwnedBy(@Param("studentId") Long studentId,
                                     @Param("status") ReservationStatus status,
                                     Pageable pageable);
}
