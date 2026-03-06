package dr.portfolio.repositories;

import dr.portfolio.domain.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CashTransactionRepository
        extends JpaRepository<CashTransaction, UUID> {

    List<CashTransaction> findByAccountIdOrderByTransactionDateAsc(UUID accountId);
    
    @Query("""
    		select coalesce(sum(c.amount), 0)
    		from CashTransaction c
    		where c.account.id = :accountId
    		  and c.transactionType = dr.portfolio.domain.CashTransactionType.DIVIDEND
    		  and c.symbol = :symbol
    		""")
    		BigDecimal sumDividendsByAccountAndSymbol(UUID accountId, String symbol);
    
    @Query("""
    	    select coalesce(sum(c.amount), 0)
    	    from CashTransaction c
    	    where c.account.id = :accountId
    	      and c.transactionType = dr.portfolio.domain.CashTransactionType.DIVIDEND
    	      and c.symbol = :symbol
    	      and year(c.transactionDate) = :year
    	""")
    	BigDecimal sumDividendsByAccountAndSymbolAndYear(
    	        @Param("accountId") UUID accountId,
    	        @Param("symbol") String symbol,
    	        @Param("year") int year
    	);
}

