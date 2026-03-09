package dr.portfolio.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class OptionTradeServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private CashTransactionRepository cashTransactionRepository;

    @Mock
    private HoldingRebuildService holdingRebuildService;

    @InjectMocks
    private OptionTradeService optionTradeService;

    private UUID accountId;
    private Account account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();

        account = new Account();
        account.setId(accountId);
        account.setName("Test Account");
    }

    @Test
    void buyOption_createsTrade_savesCashTransaction_andRebuildsHolding() {
        // arrange
        String underlying = "AAPL";
        OptionType optionType = OptionType.CALL;
        LocalDate expiration = LocalDate.of(2026, 6, 19);
        BigDecimal strikePrice = new BigDecimal("150");
        int quantity = 2;
        BigDecimal price = new BigDecimal("2.50");
        TradeType tradeType = TradeType.BUY;
        LocalDateTime tradeDate = LocalDateTime.of(2026, 3, 9, 10, 0);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(holdingRepository.findByAccountIdAndSymbol(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());

        when(holdingRepository.save(any(Holding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(cashTransactionRepository.save(any(CashTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        optionTradeService.recordOptionTrade(
                accountId,
                underlying,
                optionType,
                expiration,
                strikePrice,
                quantity,
                price,
                tradeType,
                tradeDate
        );

        // assert trade saved
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());

        Trade savedTrade = tradeCaptor.getValue();
        assertEquals(quantity, savedTrade.getQuantity());
        assertEquals(price.doubleValue(), savedTrade.getPrice());
        assertEquals(tradeType, savedTrade.getTradeType());
        assertEquals(tradeDate, savedTrade.getTradeDate());
        assertNotNull(savedTrade.getHolding());
        assertEquals(InstrumentType.OPTION, savedTrade.getHolding().getType());

        // assert cash transaction saved
        ArgumentCaptor<CashTransaction> cashCaptor = ArgumentCaptor.forClass(CashTransaction.class);
        verify(cashTransactionRepository).save(cashCaptor.capture());

        CashTransaction cashTxn = cashCaptor.getValue();
        assertEquals(CashTransactionType.BUY, cashTxn.getTransactionType());

        // 2 contracts * 2.50 * 100 = 500.00, BUY should be negative
        assertEquals(new BigDecimal("-500.00"), cashTxn.getAmount().setScale(2));
        assertEquals(tradeDate, cashTxn.getTransactionDate());

        // assert holding rebuild called
        verify(holdingRebuildService, times(1)).rebuildHolding(any(Holding.class));
    }

    @Test
    void sellOption_rebuildsHolding_afterSavingTrade() {
        // arrange
        String optionSymbol = "AAPL260619C150";
        Holding existingHolding = new Holding();
        existingHolding.setId(UUID.randomUUID());
        existingHolding.setAccount(account);
        existingHolding.setSymbol(optionSymbol);
        existingHolding.setType(InstrumentType.OPTION);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(holdingRepository.findByAccountIdAndSymbol(any(UUID.class), anyString()))
                .thenReturn(Optional.of(existingHolding));

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(cashTransactionRepository.save(any(CashTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act
        optionTradeService.recordOptionTrade(
                accountId,
                "AAPL",
                OptionType.CALL,
                LocalDate.of(2026, 6, 19),
                new BigDecimal("150"),
                1,
                new BigDecimal("1.25"),
                TradeType.SELL,
                LocalDateTime.of(2026, 3, 9, 11, 0)
        );

        // assert
        verify(tradeRepository).save(any(Trade.class));
        verify(holdingRebuildService).rebuildHolding(existingHolding);
        verify(cashTransactionRepository).save(any(CashTransaction.class));
    }
}