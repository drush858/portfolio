package dr.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.domain.Holding;
import dr.portfolio.domain.InstrumentType;
import dr.portfolio.domain.OptionType;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.CashTransactionRepository;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.TradeRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class OptionTradeService {

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final CashTransactionRepository cashTransactionRepository;

    public OptionTradeService(
    		AccountRepository accountRepository,
    		HoldingRepository holdingRepository,
    		TradeRepository tradeRepository,
    		CashTransactionRepository cashTransactionRepository) {
    	
    	this.accountRepository = accountRepository;
    	this.holdingRepository = holdingRepository;
    	this.tradeRepository = tradeRepository;
    	this.cashTransactionRepository = cashTransactionRepository;
    }
    
    public void recordOptionTrade(
        UUID accountId,
        String underlyingSymbol,
        OptionType optionType,
        LocalDate expiration,
        BigDecimal strikePrice,
        int quantity,
        BigDecimal price,
        TradeType tradeType,
        LocalDateTime tradeDate
    ) {
    	
    	if (tradeType != TradeType.BUY
    		    && tradeType != TradeType.SELL
    		    && tradeType != TradeType.OPTION_EXPIRE) {

    		throw new IllegalArgumentException(
    			"Invalid TradeType for option trade: " + tradeType
    		);
    	}

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Option quantity must be positive");
        }
        
        // Build OCC-style option symbol
        String optionSymbol = buildOptionSymbol(
                underlyingSymbol,
                expiration,
                optionType,
                strikePrice
        );

        // Find or create the OPTION holding
        Holding holding = holdingRepository
                .findByAccountIdAndSymbol(accountId, optionSymbol)
                .orElseGet(() -> createOptionHolding(
                        account,
                        optionSymbol,
                        expiration,
                        strikePrice,
                        optionType
                ));

        holdingRepository.save(holding);
        
        // Record immutable trade
        Trade trade = new Trade();
        trade.setHolding(holding);
        trade.setSymbol(optionSymbol);
        trade.setQuantity(quantity);
        trade.setPrice(price.doubleValue());
        trade.setTradeDate(tradeDate);
        trade.setTradeType(tradeType);

        tradeRepository.save(trade);

        // Cash impact (options = 100 multiplier)
        BigDecimal gross =
        	price
        	  .multiply(BigDecimal.valueOf(quantity))
        	  .multiply(BigDecimal.valueOf(100));
 
        BigDecimal cashAmount = switch (tradeType) {
            case BUY -> gross.negate();
            case SELL -> gross;
            case OPTION_EXPIRE -> BigDecimal.ZERO;
            default -> throw new IllegalArgumentException(
                "Unsupported TradeType for option: " + tradeType
            );
        };
                
        if (cashAmount.compareTo(BigDecimal.ZERO) != 0.0) {
        
        	cashAmount = cashAmount.setScale(4, RoundingMode.HALF_UP);
        	
        	CashTransactionType cashType = switch (tradeType) {
	           case BUY -> CashTransactionType.BUY;
	           case SELL -> CashTransactionType.SELL;
	           default -> throw new IllegalArgumentException("No cash type for: " + tradeType);
            };

            String desc =
                    "Option " + tradeType + " " + optionSymbol
                    + " x" + quantity
                    + " @ " + price;
            
        	cashTransactionRepository.save(
	        	new CashTransaction(
	        		account,
	        		cashType,
	                cashAmount,   // cash out
	                optionSymbol,
	                tradeDate,
	                desc
	            )
	        );
        }
    }

    private Holding createOptionHolding(
            Account account,
            String symbol,
            LocalDate expiration,
            BigDecimal strikePrice,
            OptionType optionType
    ) {
        Holding holding = new Holding();
        //holding.setId(UUID.randomUUID());
        holding.setAccount(account);
        holding.setSymbol(symbol);
        holding.setType(InstrumentType.OPTION);
        holding.setExpiration(expiration);
        holding.setStrikePrice(strikePrice);
        holding.setOptionType(optionType);

        return holding;
    }

    private String buildOptionSymbol(
            String underlying,
            LocalDate expiration,
            OptionType optionType,
            BigDecimal strike
    ) {
        return underlying.toUpperCase()
                + expiration.format(DateTimeFormatter.ofPattern("yyMMdd"))
                + optionType.name().charAt(0)
                + strike.stripTrailingZeros().toPlainString();
    }
    
}