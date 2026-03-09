package dr.portfolio.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.OptionType;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.domain.User;
import dr.portfolio.domain.Holding;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.CashTransactionRepository;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.TradeRepository;
import dr.portfolio.repositories.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OptionTradeServiceIntegrationTest {

    @Autowired
    private OptionTradeService optionTradeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private CashTransactionRepository cashTransactionRepository;

    @Test
    void buyOption_rebuildsHoldingQuantityAndAvgCost() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("secret");
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setName("Test Account");
        account.setNumber("12345");
        account.setActive(true);
        accountRepository.save(account);

        UUID accountId = account.getId();

        String underlyingSymbol = "AAPL";
        OptionType optionType = OptionType.CALL;
        LocalDate expiration = LocalDate.of(2026, 6, 19);
        BigDecimal strikePrice = new BigDecimal("150");
        int quantity = 2;
        BigDecimal price = new BigDecimal("2.50");
        TradeType tradeType = TradeType.BUY;
        LocalDateTime tradeDate = LocalDateTime.of(2026, 3, 9, 10, 0);

        // Act
        optionTradeService.recordOptionTrade(
                accountId,
                underlyingSymbol,
                optionType,
                expiration,
                strikePrice,
                quantity,
                price,
                tradeType,
                tradeDate
        );

        // Assert holding exists
        String optionSymbol = "AAPL260619C150";
        Optional<Holding> maybeHolding =
                holdingRepository.findByAccountIdAndSymbol(accountId, optionSymbol);

        assertTrue(maybeHolding.isPresent(), "Holding should have been created");

        Holding holding = maybeHolding.get();

        // The rebuild should have updated cached values
        assertEquals(2, holding.getQuantity(), "Holding quantity should be rebuilt to 2 contracts");
        assertEquals(2.50, holding.getAvgCost(), 0.0001, "Avg cost should match option premium");

        // Assert trade was saved
        var trades = tradeRepository.findByHoldingOrderByTradeDateAscIdAsc(holding);
        assertEquals(1, trades.size(), "One trade should exist");

        Trade trade = trades.get(0);
        assertEquals(TradeType.BUY, trade.getTradeType());
        assertEquals(2, trade.getQuantity());
        assertEquals(2.50, trade.getPrice(), 0.0001);
        assertEquals(optionSymbol, trade.getSymbol());
    }

    @Test
    void buyThenSellOption_reducesHoldingQuantity() {
        // Arrange
        User user = new User();
        user.setUsername("testuser2");
        user.setEmail("test2@example.com");
        user.setPassword("secret");
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setName("Options Account");
        account.setNumber("ABC123");
        account.setActive(true);
        accountRepository.save(account);

        UUID accountId = account.getId();

        LocalDate expiration = LocalDate.of(2026, 6, 19);
        BigDecimal strikePrice = new BigDecimal("150");

        // Buy 2 contracts
        optionTradeService.recordOptionTrade(
                accountId,
                "AAPL",
                OptionType.CALL,
                expiration,
                strikePrice,
                2,
                new BigDecimal("2.50"),
                TradeType.BUY,
                LocalDateTime.of(2026, 3, 9, 10, 0)
        );

        // Sell 1 contract
        optionTradeService.recordOptionTrade(
                accountId,
                "AAPL",
                OptionType.CALL,
                expiration,
                strikePrice,
                1,
                new BigDecimal("3.00"),
                TradeType.SELL,
                LocalDateTime.of(2026, 3, 10, 10, 0)
        );

        // Assert
        String optionSymbol = "AAPL260619C150";
        Holding holding = holdingRepository.findByAccountIdAndSymbol(accountId, optionSymbol)
                .orElseThrow();

        assertEquals(1, holding.getQuantity(), "Holding quantity should be reduced to 1");
        assertEquals(2.50, holding.getAvgCost(), 0.0001, "Remaining open contract should keep original FIFO basis");
    }
}