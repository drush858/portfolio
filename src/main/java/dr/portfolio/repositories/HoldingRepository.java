package dr.portfolio.repositories;

import java.util.List;
import java.util.Optional;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import dr.portfolio.domain.Holding;
import jakarta.persistence.LockModeType;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
	
	public List<Holding> findByAccount_Id(UUID id);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Holding> findByAccount_IdAndSymbol(UUID id, String symbol);

	@Query("select distinct h.symbol from Holding h")
    List<String> findAllDistinctSymbols();
	
	Optional<Holding> findByAccountIdAndSymbol(UUID accountId, String symbol);
}


