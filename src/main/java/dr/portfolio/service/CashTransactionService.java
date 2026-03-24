package dr.portfolio.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.dto.CashLedgerRow;
import dr.portfolio.dto.CashSummary;
import dr.portfolio.dto.CashTransactionEdit;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.CashTransactionRepository;
import dr.portfolio.repositories.TradeRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class CashTransactionService {

	private final CashTransactionRepository cashTransactionRepository;
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
	
	public CashTransactionService(
			CashTransactionRepository cashTransactionRepository,
			AccountRepository accountRepository,
			TradeRepository tradeRepository) {
		
		this.cashTransactionRepository = cashTransactionRepository;
		this.accountRepository = accountRepository;
		this.tradeRepository = tradeRepository;
	}
	
	public List<CashTransaction> findForAccount(UUID accountId) {
		
		return cashTransactionRepository.findByAccountIdOrderByTransactionDateAsc(accountId);
	}
	
	public CashTransactionEdit getEditModel(UUID txnId) {
        CashTransaction tx = cashTransactionRepository.findById(txnId)
            .orElseThrow(() -> new IllegalArgumentException("Cash transaction not found"));

        CashTransactionEdit dto = new CashTransactionEdit();
        dto.setId(tx.getId());
        dto.setTransactionDate(tx.getTransactionDate());
        dto.setTransactionType(tx.getTransactionType());
        dto.setAmount(tx.getAmount().abs()); // if you store negatives for outflows
        dto.setSymbol(tx.getSymbol());
        dto.setDescription(tx.getDescription());
        return dto;
    }
	
	public void update(
			UUID id, 
			LocalDateTime transactionDate, 
			CashTransactionType transactionType, 
			BigDecimal amount, 
			String symbol, 
			String description)
	{
        CashTransaction tx = cashTransactionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cash transaction not found"));

        if (!tx.isEditable()) {
            throw new IllegalStateException("This transaction is generated from trades and cannot be edited.");
        }

        tx.setTransactionDate(transactionDate);
        tx.setTransactionType(transactionType);
        tx.setSymbol(blankToNull(symbol));
        tx.setDescription(blankToNull(description));

        // normalize sign (optional policy)
        tx.setAmount(applySign(transactionType, amount));

        cashTransactionRepository.save(tx);
    }
	
	private BigDecimal applySign(CashTransactionType type, BigDecimal amount) {
        // choose your rule. Example:
        return switch (type) {
            case BUY, FEE, TRANSFER_OUT -> amount.negate();
            default -> amount; // inflows
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
	
    public List<String> findSymbols(UUID accountId) {
    	
    	List<String> symbols = tradeRepository.findDistinctSymbolsByAccountId(accountId)
    		.stream()
    		.filter(s -> !s.matches(".*\\d{6}[CP].*"))
    		.toList();
    	
    	return symbols;
    }
    
    public Page<CashLedgerRow> buildLedger(UUID accountId, int page, int size, String symbol) {

        Pageable pageable = PageRequest.of(page, size);

        Page<CashTransaction> txPage;
        
        if (symbol != null && !symbol.isBlank()) {
            txPage = cashTransactionRepository
                    .findByAccountIdAndSymbolOrderByTransactionDateDescIdDesc(
                            accountId,
                            symbol.trim().toUpperCase(),
                            pageable
                    );
        } else {
            txPage = cashTransactionRepository
                    .findByAccountIdOrderByTransactionDateDesc(accountId, pageable);
        }

        List<CashTransaction> pageTxs = new ArrayList<>(txPage.getContent());

        if (pageTxs.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, txPage.getTotalElements());
        }

        // Newest-first page returned by DB
        // Find oldest txn on this page
        CashTransaction oldestOnPage = pageTxs.get(pageTxs.size() - 1);

        BigDecimal startingBalance;
        if (symbol != null && !symbol.isBlank()) {
            startingBalance = cashTransactionRepository.sumAmountsByAccountAndSymbolBeforeDateAndId(
                    accountId,
                    symbol.trim().toUpperCase(),
                    oldestOnPage.getTransactionDate(),
                    oldestOnPage.getId()
            );
        } else {
            startingBalance = cashTransactionRepository.sumAmountsBeforeDateAndId(
                    accountId,
                    oldestOnPage.getTransactionDate(),
                    oldestOnPage.getId()
            );
        }

        // Reverse to chronological order for running balance calculation
        Collections.reverse(pageTxs);

        List<CashLedgerRow> rows = new ArrayList<>();
        BigDecimal runningBalance = startingBalance;

        for (CashTransaction tx : pageTxs) {
            runningBalance = runningBalance.add(tx.getAmount());

            rows.add(new CashLedgerRow(
                tx.getId(),
                tx.getTransactionDate(),
                tx.getTransactionType(),
                tx.getSymbol(),
                tx.getDescription(),
                tx.getAmount(),
                runningBalance.doubleValue(),
                tx.isEditable()
            ));
        }

        // Reverse back to newest-first for display
        Collections.reverse(rows);

        return new PageImpl<>(rows, pageable, txPage.getTotalElements());
    }

	public CashSummary calculateSummary(UUID accountId) {

        List<CashTransaction> txs =
        		cashTransactionRepository.findByAccountIdOrderByTransactionDateAsc(accountId);

        double totalIn = 0.0;
        double totalOut = 0.0;

        for (CashTransaction tx : txs) {
            double amount = tx.getAmount().doubleValue();

            if (amount > 0) {
                totalIn += amount;
            } else {
                totalOut += Math.abs(amount);
            }
        }

        double currentBalance = totalIn - totalOut;

        // Starting balance is optional — set to 0 unless you
        // later add an OPENING_BALANCE transaction type
        double startingBalance = 0.0;

        return new CashSummary(
            round(startingBalance),
            round(totalIn),
            round(totalOut),
            round(currentBalance)
        );
             
	}

	private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

	public void addDividend(
			UUID accountId,
	        LocalDateTime transactionDate,
	        BigDecimal amount,
	        String symbol,
	        String description
	) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Dividend amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found: " + accountId)
                );

        CashTransaction txn = new CashTransaction();
        txn.setAccount(account);
        txn.setTransactionDate(transactionDate);
        txn.setAmount(amount);
        txn.setTransactionType(CashTransactionType.DIVIDEND);
        txn.setSymbol((symbol == null || symbol.isBlank()) ? null : symbol);
        txn.setDescription(description);

        cashTransactionRepository.save(txn);
    }
	
	public void addCash(
	        UUID accountId,
	        LocalDateTime transactionDate,
	        BigDecimal amount,
	        CashTransactionType type,
	        String description
	) {
	    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
	        throw new IllegalArgumentException("Cash amount must be positive");
	    }

	    if (type != CashTransactionType.OPENING_BALANCE
	        && type != CashTransactionType.TRANSFER_IN) {
	        throw new IllegalArgumentException("Invalid cash transaction type");
	    }

	    Account account = accountRepository.findById(accountId)
	            .orElseThrow(() ->
	                    new IllegalArgumentException("Account not found: " + accountId)
	            );

	    CashTransaction txn = new CashTransaction();
	    txn.setAccount(account);
	    txn.setTransactionDate(transactionDate);
	    txn.setAmount(amount);
	    txn.setTransactionType(type);
	    txn.setDescription(description);

	    cashTransactionRepository.save(txn);
	}
	
	public void withdraw(
            UUID accountId,
            LocalDateTime transactionDate,
            BigDecimal amount,
            String description
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        CashTransaction txn = new CashTransaction(
                account,
                CashTransactionType.TRANSFER_OUT,
                amount.negate(),       // ✅ store as negative outflow
                null,                  // symbol usually null for transfer
                transactionDate,
                (description == null || description.isBlank())
                        ? "Withdrawal"
                        : description.trim()
        );

        cashTransactionRepository.save(txn);
    }

}