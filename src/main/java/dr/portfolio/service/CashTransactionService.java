package dr.portfolio.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.dto.CashLedgerRow;
import dr.portfolio.dto.CashSummary;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.CashTransactionRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class CashTransactionService {

	private final CashTransactionRepository cashTransactionRepository;
    private final AccountRepository accountRepository;
	
	public CashTransactionService(
			CashTransactionRepository cashTransactionRepository,
			AccountRepository accountRepository ) {
		
		this.cashTransactionRepository = cashTransactionRepository;
		this.accountRepository = accountRepository;
	}
	
	public List<CashTransaction> findForAccount(UUID accountId) {
		
		return cashTransactionRepository.findByAccountIdOrderByTransactionDateAsc(accountId);
	}
	
	public List<CashLedgerRow> buildLedger(UUID accountId) {

	    List<CashTransaction> txs =
	    		cashTransactionRepository.findByAccountIdOrderByTransactionDateAsc(accountId);

	    double balance = 0;
	    List<CashLedgerRow> rows = new ArrayList<>();

	    for (CashTransaction tx : txs) {
	        balance += tx.getAmount().doubleValue();

	        rows.add(new CashLedgerRow(
	            tx.getTransactionDate(),
	            tx.getTransactionType(),
	            tx.getSymbol(),
	            tx.getDescription(),
	            tx.getAmount(),
	            balance
	        ));
	    }

	    return rows;
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
	
	@Transactional
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

}