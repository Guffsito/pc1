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

@Entity
@Table(
        name = "laboratories",
        uniqueConstraints = @UniqueConstraint(name = "uk_laboratories_name", columnNames = "name")
)
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private UserAccount manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabStatus status;

    protected Laboratory() {
        // requerido por JPA
    }

    public Laboratory(String name, String location, UserAccount manager, LabStatus status) {
        this.name = name;
        this.location = location;
        this.manager = manager;
        this.status = status;
    }

    public boolean isManagedBy(Long userId) {
        return manager != null && manager.getId().equals(userId);
    }

    public boolean isActive() {
        return status == LabStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public UserAccount getManager() {
        return manager;
    }

    public LabStatus getStatus() {
        return status;
    }

    public void setStatus(LabStatus status) {
        this.status = status;
    }
}
