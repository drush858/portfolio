package dr.portfolio.service;

import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.domain.Holding;
import dr.portfolio.domain.InstrumentType;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.TradeCreate;
import dr.portfolio.market.AlphaVantagePollingService;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.CashTransactionRepository;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.TradeRepository;
import dr.portfolio.repositories.UserRepository;

@Service
@Transactional
public class TradeService {

    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AlphaVantagePollingService alphaVantagePollingService;
    private final CashTransactionRepository cashTransactionRepository;
    private final HoldingRebuildService holdingRebuildService;

    public TradeService(
            HoldingRepository holdingRepository,
            TradeRepository tradeRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            AlphaVantagePollingService alphaVantagePollingService,
            CashTransactionRepository cashTransactionRepository,
            HoldingRebuildService holdingRebuildService) 
    {
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.alphaVantagePollingService = alphaVantagePollingService;
        this.cashTransactionRepository = cashTransactionRepository;
        this.holdingRebuildService = holdingRebuildService;
    }
    
    public List<Trade> getTradesForUser(String username) {
    	
    	return tradeRepository.findAllTradesForUserOrdered(
    			userRepository.findByUsername(username)
    	);
    }

    public void deleteById(UUID id, String username) {
    
    	List<Account> accounts = accountRepository.findByUserUsername(username);
    			//.orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    	
    	tradeRepository.deleteById(id);
    }
    
    public List<Trade> findByHoldingId(UUID holdingId) {
        Holding holding = holdingRepository.getReferenceById(holdingId);
        return tradeRepository.findByHoldingOrderByTradeDateAscIdAsc(holding);
    }

    
    public Trade buy(TradeCreate tradeCreate, String username) throws ParseException, AccessDeniedException {
    	
    	if (isOptionSymbol(tradeCreate.getSymbol())) {
    	    throw new IllegalArgumentException("Option trades must use the option trade workflow");
    	}
    	
    	Account account = accountRepository.findById(tradeCreate.getAccountId())
    	        .filter(a -> a.getUser().getUsername().equals(username))
    	        .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    	
    	validateTrade(tradeCreate.getQuantity(), tradeCreate.getPrice());

    	Holding holding = holdingRepository
                .findByAccount_IdAndSymbol(account.getId(), tradeCreate.getSymbol())
                .orElseGet(() -> createNewHolding(account, tradeCreate));

        holdingRepository.save(holding);
        
        Trade newTrade = createTrade(holding, TradeType.BUY, tradeCreate, tradeCreate.getDate().atStartOfDay());
      
        Trade trade = tradeRepository.save(newTrade);
        
        // 💰 CASH TRANSACTION
        BigDecimal totalCost =
        		BigDecimal.valueOf(trade.getPrice())
        			.multiply(BigDecimal.valueOf(trade.getQuantity()));
        
        cashTransactionRepository.save(
        	new CashTransaction(
        		account,
                CashTransactionType.BUY,
                totalCost.negate(),   // cash out
                trade.getSymbol(),
                trade.getTradeDate(),
                "Buy " + trade.getQuantity()
            )
        );
        
        alphaVantagePollingService.trackSymbol(trade.getSymbol());
        
        // recompute holding from trades
        holdingRebuildService.rebuildHolding(holding);
        
        return trade;
    }

    public Trade sell(TradeCreate tradeCreate, String username) throws ParseException, AccessDeniedException {
    	
    	if (isOptionSymbol(tradeCreate.getSymbol())) {
    	    throw new IllegalArgumentException("Option trades must use the option trade workflow");
    	}
    	
    	Account account = accountRepository.findById(tradeCreate.getAccountId())
    	        .filter(a -> a.getUser().getUsername().equals(username))
    	        .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    	
    	validateTrade(tradeCreate.getQuantity(), tradeCreate.getPrice());

        Holding holding = holdingRepository
                .findByAccount_IdAndSymbol(account.getId(), tradeCreate.getSymbol())
                .orElseThrow(() -> new IllegalStateException("No holding found to sell"));

        if (holding.getQuantity() < tradeCreate.getQuantity()) {
            throw new IllegalArgumentException("Insufficient quantity to sell");
        }

        Trade newTrade = createTrade(holding, TradeType.SELL, tradeCreate, tradeCreate.getDate().atStartOfDay());

        Trade trade = tradeRepository.save(newTrade);
        
        // 💰 CASH TRANSACTION
        BigDecimal proceeds =
            BigDecimal.valueOf(trade.getPrice())
                      .multiply(BigDecimal.valueOf(trade.getQuantity()));

        cashTransactionRepository.save(
            new CashTransaction(
                account,
                CashTransactionType.SELL,
                proceeds,            // cash in
                trade.getSymbol(),
                trade.getTradeDate(),
                "Sell " + trade.getQuantity()
            )
        );
        
        // recompute holding from trades
        holdingRebuildService.rebuildHolding(holding);
        
        return trade;
    }

    // ----------------- Helpers -----------------

    private Holding createNewHolding(Account account, TradeCreate tradeCreate) {
        Holding holding = new Holding();
        holding.setAccount(account);
        holding.setSymbol(tradeCreate.getSymbol().toUpperCase());
        holding.setQuantity(0); //tradeCreate.getQuantity());
        holding.setAvgCost(0); //tradeCreate.getPrice());
        holding.setType(InstrumentType.STOCK);
        return holding;
    }

    private Trade createTrade(
            Holding holding,
            TradeType type,
            TradeCreate tradeCreate,
            LocalDateTime date
    ) {
        Trade trade = new Trade();
        trade.setHolding(holding);
        trade.setTradeType(type);
        trade.setSymbol(tradeCreate.getSymbol().toUpperCase());
        trade.setQuantity(tradeCreate.getQuantity());
        trade.setPrice(tradeCreate.getPrice());
        trade.setTradeDate(date);
        return trade;
    }

    private void validateTrade(int quantity, double price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
    
    private boolean isOptionSymbol(String symbol) {
        return symbol != null && symbol.matches(".*\\d{6}[CP].*");
    }
    
    public Trade getTrade(UUID id) {
    	
    	return tradeRepository.getReferenceById(id);
    }

    public List<Trade> getTradesForAccount(UUID accountId) {
        return tradeRepository.findAllTradesForAccount(accountId);
    }

    @Transactional(readOnly = true)
    public List<Trade> getTradesForHolding(UUID holdingId, String username) {
    	
        Holding holding = holdingRepository.getReferenceById(holdingId);
        		//.orElseThrow(() -> new IllegalArgumentException("Holding not found"));
        
        String ownerUsername =
                holding.getAccount().getUser().getUsername();
        
        if (!ownerUsername.equals(username)) {
            throw new AccessDeniedException("Unauthorized access to holding");
        }
        return tradeRepository.findByHoldingOrderByTradeDateDescIdAsc(holding);
    }

}

