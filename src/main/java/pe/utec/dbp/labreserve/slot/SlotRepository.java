package pe.utec.dbp.labreserve.slot;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.SlotStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<EquipmentSlot, Long> {

    /**
     * Turnos vigentes del mismo equipo dentro de un laboratorio. Sirve de base para
     * la validacion de solapamiento; los cancelados quedan fuera porque liberan el horario.
     */
    @Query("""
            SELECT s FROM EquipmentSlot s
            WHERE s.laboratory.id = :laboratoryId
              AND UPPER(s.equipmentCode) = UPPER(:equipmentCode)
              AND s.status <> pe.utec.dbp.labreserve.model.SlotStatus.CANCELLED
            """)
    List<EquipmentSlot> findLiveSlotsForEquipment(@Param("laboratoryId") Long laboratoryId,
                                                  @Param("equipmentCode") String equipmentCode);

    @Query(value = """
            SELECT s FROM EquipmentSlot s
            JOIN FETCH s.laboratory
            WHERE s.status = :status
              AND s.startTime > :now
              AND (:laboratoryId IS NULL OR s.laboratory.id = :laboratoryId)
              AND (:equipmentCode IS NULL OR UPPER(s.equipmentCode) LIKE UPPER(CONCAT('%', :equipmentCode, '%')))
              AND (:from IS NULL OR s.startTime >= :from)
            ORDER BY s.startTime ASC
            """,
            countQuery = """
            SELECT COUNT(s) FROM EquipmentSlot s
            WHERE s.status = :status
              AND s.startTime > :now
              AND (:laboratoryId IS NULL OR s.laboratory.id = :laboratoryId)
              AND (:equipmentCode IS NULL OR UPPER(s.equipmentCode) LIKE UPPER(CONCAT('%', :equipmentCode, '%')))
              AND (:from IS NULL OR s.startTime >= :from)
            """)
    Page<EquipmentSlot> searchOpenSlots(@Param("laboratoryId") Long laboratoryId,
                                        @Param("equipmentCode") String equipmentCode,
                                        @Param("from") ZonedDateTime from,
                                        @Param("now") ZonedDateTime now,
                                        @Param("status") SlotStatus status,
                                        Pageable pageable);

    /**
     * Bloquea la fila del turno para que dos reservas simultaneas no pasen
     * la validacion de capacidad al mismo tiempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EquipmentSlot s WHERE s.id = :id")
    Optional<EquipmentSlot> lockById(@Param("id") Long id);
}
