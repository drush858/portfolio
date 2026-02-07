package dr.portfolio.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dr.portfolio.domain.Holding;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.User;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
	
	List<Trade> findByHoldingOrderByTradeDateAscIdAsc(Holding holding);
	
	List<Trade> findByHoldingOrderByTradeDateDescIdAsc(Holding holding);
	
	List<Trade> findByHolding_IdOrderByTradeDateAscIdAsc(UUID id);
	
	@Query("""
	        SELECT t
	        FROM Trade t
	        JOIN t.holding h
	        WHERE h.account.id = :id
	        ORDER BY t.tradeDate
	    """)
	List<Trade> findAllTradesForAccount(@Param("id") UUID id);
		
	@Query("""
			SELECT t
			FROM Trade t
			JOIN t.holding h
			JOIN h.account a
			WHERE a.user = :user
			ORDER BY t.tradeDate, t.id
			""")
	List<Trade> findAllTradesForUserOrdered(User user); //@Param("username") String username);
	
	
	List<Trade> findAllByOrderByTradeDateAsc();
	
	@Query("""
	        select distinct t.symbol
	        from Trade t
	        where t.holding.account.id = :accountId
	    """)
	List<String> findDistinctSymbolsByAccountId(UUID accountId);

}
