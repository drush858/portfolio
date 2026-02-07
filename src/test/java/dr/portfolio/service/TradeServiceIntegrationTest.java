package dr.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import dr.portfolio.domain.Account;
import dr.portfolio.domain.Holding;
import dr.portfolio.domain.User;
import dr.portfolio.dto.TradeCreate;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.UserRepository;

@ActiveProfiles("local")
@DataJpaTest
@Import(TradeService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TradeServiceIntegrationTest {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;
    
    private Account account;
    private Holding holding;
    private User user;
    
    @BeforeEach
    void setUp() {
        
        user = new User();
        user.setUsername("rushda");
        user.setEmail("rushda@gmail.com");
        user.setPassword("password");
        userRepository.save(user);
        
        account = new Account();
        account.setName("AccountOne");
        account.setNumber("3232");
        account.setUser(user);
        account.setActive(true);
        accountRepository.save(account);
        
        holding = new Holding();
        holding.setAccount(account);
        holding.setSymbol("BMY");
        holding.setQuantity(10);
        holding.setAvgCost(100.50);
        holdingRepository.save(holding);
    }

    @Test
    void buyStock_existingHolding_updatesQuantityAndAvgCost() throws ParseException {

    	TradeCreate tradeCreate = new TradeCreate();
    	tradeCreate.setSymbol("BMY");
    	
    	Trade trade = tradeService.buy(tradeCreate, "rushda");

        assertEquals(15, holding.getQuantity());

        assertEquals(
                107.0,
                holding.getAvgCost()
        );

        assertEquals(TradeType.BUY, trade.getTradeType());
    }
    
    @Test
    void buyStock_createsNewHoldingWhenMissing() throws ParseException {

    	TradeCreate tradeCreate = new TradeCreate();
    	tradeCreate.setSymbol("BMY");
    	
    	Trade trade = tradeService.buy(tradeCreate, "rushda");

        Holding created = trade.getHolding();

        assertEquals(10, created.getQuantity());
        assertEquals(
                50,
                created.getAvgCost()
        );
    }

    @Test
    void sellStock_reducesQuantity() {

    	TradeCreate tradeCreate = new TradeCreate();
    	tradeCreate.setSymbol("BMY");
    	
        Trade trade = new Trade();
		try {
			trade = tradeService.sell(tradeCreate, "rushda");
		} catch (AccessDeniedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        assertEquals(6, holding.getQuantity());
        assertEquals(TradeType.SELL, trade.getTradeType());
    }
    
    @Test
    void sellStock_insufficientQuantity_throwsException() {

    	TradeCreate tradeCreate = new TradeCreate();
    	tradeCreate.setSymbol("BMY");
    	
        assertThrows(IllegalArgumentException.class, () ->
                tradeService.sell(tradeCreate, "rushda")
        );
    }

    @Test
    @Order(5)
    void buyStock_invalidQuantity_throwsException()  throws ParseException {

    	TradeCreate tradeCreate = new TradeCreate();
    	tradeCreate.setSymbol("BMY");
    	
        assertThrows(IllegalArgumentException.class, () ->
                tradeService.buy(tradeCreate, "rushda")
        );
    }
}


