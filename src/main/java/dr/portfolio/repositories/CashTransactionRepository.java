package dr.portfolio.repositories;

import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.domain.Trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashTransactionRepository
        extends JpaRepository<CashTransaction, UUID> {

	Optional<CashTransaction> findByTrade_Id(UUID tradeId);
	
	Optional<CashTransaction> findByTrade(Trade trade);
	 
	Page<CashTransaction> findByAccountIdAndSymbolOrderByTransactionDateDescIdDesc(
	        UUID accountId,
	        String symbol,
	        Pageable pageable
	);
	
    Page<CashTransaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId, Pageable pageable);
    
    List<CashTransaction> findByAccountIdOrderByTransactionDateAsc(UUID accountId);
    
    @Query("""
    	    select coalesce(sum(c.amount), 0)
    	    from CashTransaction c
    	    where c.account.id = :accountId
    	      and c.transactionDate < :beforeDate
    	""")
    	BigDecimal sumAmountsBeforeDateAndId(
    	        @Param("accountId") UUID accountId,
    	        @Param("beforeDate") LocalDateTime beforeDate,
    	        @Param("beforeId") UUID beforeId
    	);
    
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
    
    @Query("""
            select coalesce(sum(c.amount), 0)
            from CashTransaction c
            where c.account.id in :accountIds
              and c.transactionType = :type
              and c.transactionDate between :start and :end
        """)
        BigDecimal sumByAccountsTypeAndDateRange(
            @Param("accountIds") List<UUID> accountIds,
            @Param("type") CashTransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
        );

        @Query("""
            select c
            from CashTransaction c
            where c.account.id in :accountIds
              and c.transactionType in :types
              and c.transactionDate between :start and :end
            order by c.transactionDate
        """)
        List<CashTransaction> findIncomeTransactionsByAccountsAndDateRange(
            @Param("accountIds") List<UUID> accountIds,
            @Param("types") List<CashTransactionType> types,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
        );
        
        @Query("""
        	    select coalesce(sum(c.amount), 0)
        	    from CashTransaction c
        	    where c.account.id = :accountId
        	      and c.symbol = :symbol
        	      and (
        	            c.transactionDate < :beforeDate
        	            or (c.transactionDate = :beforeDate and c.id < :beforeId)
        	          )
        	""")
        	BigDecimal sumAmountsByAccountAndSymbolBeforeDateAndId(
        	        @Param("accountId") UUID accountId,
        	        @Param("symbol") String symbol,
        	        @Param("beforeDate") LocalDateTime beforeDate,
        	        @Param("beforeId") UUID beforeId
        	);
}

