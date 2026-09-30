package pe.utec.dbp.labreserve.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "equipment_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_slot_equipment_start",
                columnNames = {"laboratory_id", "equipment_code", "start_time"}
        )
)
public class EquipmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @Column(name = "equipment_code", nullable = false, length = 50)
    private String equipmentCode;

    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private ZonedDateTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status;

    protected EquipmentSlot() {
        // requerido por JPA
    }

    public EquipmentSlot(Laboratory laboratory,
                         String equipmentCode,
                         ZonedDateTime startTime,
                         ZonedDateTime endTime,
                         Integer capacity) {
        this.laboratory = laboratory;
        this.equipmentCode = equipmentCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.status = SlotStatus.AVAILABLE;
    }

    /**
     * Dos intervalos se solapan si cada uno empieza antes de que termine el otro.
     * Los extremos que solo se tocan (fin == inicio) no cuentan como solapamiento.
     */
    public boolean overlaps(ZonedDateTime otherStart, ZonedDateTime otherEnd) {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    public Long getId() {
        return id;
    }

    public Laboratory getLaboratory() {
        return laboratory;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
