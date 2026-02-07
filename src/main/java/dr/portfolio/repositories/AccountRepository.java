package dr.portfolio.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import dr.portfolio.domain.Account;

public interface AccountRepository  extends JpaRepository<Account, UUID> {
		
	Optional<Account> findByNumberAndUserUsername(
		String number,
	    String username
	);
	
	Optional<Account> findByIdAndUserUsername(
			UUID id,
			String username
	);
	
	List<Account> findByUser_Id(UUID id);
	
	List<Account> findByUserUsername(String username);
	
	List<Account> findByUser_IdOrderByNameAsc(UUID id);

	//@Query("""
		//    select a from Account a
		//    join fetch a.holdings
		//    where a.id = :userId
	//	""`)
	List<Account> findWithHoldingsByUserId(UUID userId);

}
