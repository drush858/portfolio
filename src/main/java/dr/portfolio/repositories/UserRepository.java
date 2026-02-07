package dr.portfolio.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import dr.portfolio.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	
	User findByUsername(String username);
	boolean existsByUsername(String username);
}
