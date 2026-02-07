package dr.portfolio.repositories;

import dr.portfolio.domain.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashTransactionRepository
        extends JpaRepository<CashTransaction, UUID> {

    List<CashTransaction> findByAccountIdOrderByTransactionDateAsc(UUID accountId);
}

