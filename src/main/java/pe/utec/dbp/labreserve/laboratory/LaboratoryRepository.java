package pe.utec.dbp.labreserve.laboratory;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utec.dbp.labreserve.model.Laboratory;

import java.util.Optional;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    boolean existsByName(String name);

    Optional<Laboratory> findByName(String name);
}
