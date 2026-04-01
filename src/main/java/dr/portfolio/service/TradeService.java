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
import dr.portfolio.domain.CashTxnSource;
import dr.portfolio.domain.Holding;
import dr.portfolio.domain.InstrumentType;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.TradeCreate;
import dr.portfolio.dto.TradeDetailView;
import dr.portfolio.dto.TradeEditForm;
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
    
    @Transactional(readOnly = true)
    public CashTransaction getCashTransactionForTrade(UUID tradeId, String username) throws AccessDeniedException {
        Trade trade = getTradeForUser(tradeId, username);

        return cashTransactionRepository.findByTrade_Id(trade.getId()).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public TradeDetailView getTradeDetail(UUID tradeId, String username) throws AccessDeniedException {
        Trade trade = getTradeForUser(tradeId, username);
        CashTransaction cashTransaction = cashTransactionRepository.findByTrade_Id(trade.getId()).orElse(null);

        return new TradeDetailView(trade, cashTransaction);
    }
    
    @Transactional
    public void updateSellTrade(TradeEditForm form, String username)
            throws IllegalArgumentException, AccessDeniedException
    {
        Trade trade = tradeRepository.findById(form.getTradeId())
                .orElseThrow(() -> new IllegalArgumentException("Trade not found"));

        Holding holding = trade.getHolding();
        Account account = holding.getAccount();

        if (!account.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Unauthorized trade edit");
        }

        if (trade.getTradeType() != TradeType.SELL) {
            throw new IllegalArgumentException("Only SELL trades can be edited here");
        }

        validateTrade(form.getQuantity(), form.getPrice());

        trade.setQuantity(form.getQuantity());
        trade.setPrice(form.getPrice());
        trade.setTradeDate(form.getTradeDate().atStartOfDay());

        Trade savedTrade = tradeRepository.save(trade);

        BigDecimal proceeds = BigDecimal.valueOf(savedTrade.getPrice())
                .multiply(BigDecimal.valueOf(savedTrade.getQuantity()));

        CashTransaction cashTransaction = cashTransactionRepository.findByTrade_Id(savedTrade.getId())
                .orElseGet(() -> {
                    CashTransaction tx = new CashTransaction();
                    tx.setAccount(account);
                    tx.setTrade(savedTrade);
                    tx.setSource(CashTxnSource.TRADE);
                    tx.setEditable(false);
                    return tx;
                });

        cashTransaction.setTransactionType(CashTransactionType.SELL);
        cashTransaction.setAmount(proceeds);
        cashTransaction.setSymbol(savedTrade.getSymbol());
        cashTransaction.setTransactionDate(savedTrade.getTradeDate());
        cashTransaction.setDescription("Sell " + savedTrade.getQuantity() + " " + savedTrade.getSymbol());
        cashTransaction.setSource(CashTxnSource.TRADE);
        cashTransaction.setEditable(false);

        cashTransactionRepository.save(cashTransaction);

        holdingRebuildService.rebuildHolding(holding);
    }
    
    public List<Trade> getTradesForUser(String username) {
    	
    	return tradeRepository.findAllTradesForUserOrdered(
    			userRepository.findByUsername(username)
    	);
    }

    @Transactional
    public void deleteById(UUID id, String username) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found"));

        Account account = trade.getHolding().getAccount();

        if (!account.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Unauthorized");
        }

        cashTransactionRepository.findByTrade_Id(trade.getId())
                .ifPresent(cashTransactionRepository::delete);

        Holding holding = trade.getHolding();

        tradeRepository.delete(trade);

        holdingRebuildService.rebuildHolding(holding);
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
        		trade,
                CashTransactionType.BUY,
                totalCost.negate(),   // cash out
                trade.getSymbol(),
                trade.getTradeDate(),
                "Buy " + trade.getQuantity() + " " + trade.getSymbol()
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
                trade,
                CashTransactionType.SELL,
                proceeds,            // cash in
                trade.getSymbol(),
                trade.getTradeDate(),
                "Sell " + trade.getQuantity() + " " + trade.getSymbol()
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
    
    public Trade getTradeForUser(UUID id, String username) throws AccessDeniedException {
		
		Trade trade = tradeRepository.getReferenceById(id);
		
		Holding holding = trade.getHolding();
	    Account account = holding.getAccount();

	    if (!account.getUser().getUsername().equals(username)) {
	       throw new AccessDeniedException("Unauthorized");
	    }
	    
	    return trade;
    
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

