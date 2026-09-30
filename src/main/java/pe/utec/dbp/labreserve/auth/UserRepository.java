package pe.utec.dbp.labreserve.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utec.dbp.labreserve.model.UserAccount;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
