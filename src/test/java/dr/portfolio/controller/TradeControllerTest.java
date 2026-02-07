package dr.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.Holding;
import dr.portfolio.domain.User;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.TradeRequest;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.UserRepository;
import dr.portfolio.service.TradeService;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
//@WebMvcTest(TradeController.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;
    
    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
    void buyStock_returns200() throws Exception {

        Trade trade = new Trade();
        trade.setTradeType(TradeType.BUY);
        trade.setQuantity(10);
        trade.setPrice(150);

        TradeRequest request = new TradeRequest();
        request.setAccountId(account.getId());
        request.setSymbol("BMY");

        request.setQuantity(10);
        request.setPrice(150);

        mockMvc.perform(post("/api/trades/buy")
                		.contentType(MediaType.APPLICATION_JSON)
                		.content(objectMapper.writeValueAsString(request)))
        		.andExpect(status().isOk())
        		.andExpect(jsonPath("$.tradeAction").value("BUY"))
        		.andExpect(jsonPath("$.quantity").value(10))
        		.andExpect(jsonPath("$.price").value(150));
    }

    @Test
    void buyStock_invalidRequest_returns400() throws Exception {

        TradeRequest request = new TradeRequest();
        request.setAccountId(account.getId());
        request.setSymbol("BMY");
        request.setQuantity(0);
        request.setPrice(150);

        mockMvc.perform(post("/api/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sellStock_returns200() throws Exception {

        Trade trade = new Trade();
        trade.setTradeType(TradeType.SELL);
        trade.setQuantity(5);
        trade.setPrice(180);

        when(tradeService.sell(
                any(), any() ))
                .thenReturn(trade);

        TradeRequest request = new TradeRequest();
        request.setAccountId(account.getId());
        request.setSymbol("BMY");
        request.setQuantity(5);
        request.setPrice(180);

        mockMvc.perform(post("/api/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeAction").value("SELL"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.price").value(180));
    }

    @Test
    void sellStock_insufficientQuantity_returns400() throws Exception {

        TradeRequest request = new TradeRequest();
        request.setAccountId(account.getId());
        request.setSymbol("BMY");
        request.setQuantity(100);
        request.setPrice(200);

        mockMvc.perform(post("/api/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @RestControllerAdvice
    public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<String> handleIllegalArgument(
                IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());
        }
    }
}

    