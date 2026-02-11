package dr.portfolio.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.Holding;
import dr.portfolio.domain.Trade;
import dr.portfolio.dto.HoldingsResult;
import dr.portfolio.dto.TradeCreate;
import dr.portfolio.dto.market.CachedPrice;
import dr.portfolio.market.MarketPriceService;
import dr.portfolio.market.PriceCache;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.TradeRepository;
import dr.portfolio.service.HoldingsService;

@Controller
@RequestMapping("/holdings")
public class HoldingController {

    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final HoldingsService holdingsService;
    private final PriceCache priceCache;
    private final MarketPriceService marketPriceService;

    public HoldingController(HoldingRepository holdingRepository, 
    		                 AccountRepository accountRepository,
    		                 TradeRepository tradeRepository,
    		                 HoldingsService holdingsService,
    		                 PriceCache priceCache,
    		                 MarketPriceService marketPriceService) {
        this.holdingRepository = holdingRepository;
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.holdingsService = holdingsService;
        this.priceCache = priceCache;
        this.marketPriceService = marketPriceService;
    }
    
    @GetMapping("/view/{id}")
    public String viewHoldings(Model model, Principal principal, @PathVariable UUID id) {

    	List<Trade> trades = tradeRepository.findAllTradesForAccount(id);
        
        Map<String, CachedPrice> prices =
            priceCache.getAll(); //.getLivePrices(trades);

        HoldingsResult holdingsResult =
            holdingsService.calculateHoldings(trades, prices, marketPriceService);

        Account acct = accountRepository.getReferenceById(id);

        model.addAttribute("pageTitle", acct.getName() + " Holdings");
        model.addAttribute("holdings", holdingsResult.getHoldings());
        
        model.addAttribute("totalMarketValue", holdingsResult.getTotalMarketValue());
        model.addAttribute("totalCostBasis", holdingsResult.getTotalCostBasis());
        model.addAttribute("totalGain", holdingsResult.getTotalGain());
        model.addAttribute("totalGainPercent", holdingsResult.getTotalPercentGain());
        
        model.addAttribute("holding", new Holding());
        model.addAttribute("accountName", acct.getName() + " Holdings");
        model.addAttribute("tradeCreate", new TradeCreate());
        model.addAttribute("accountId", id);
    	
        return "holdings";
    }

    @PostMapping
    public String create(@ModelAttribute Holding holding) {
    	holdingRepository.save(holding);
        return "redirect:/holdings";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
    	holdingRepository.deleteById(id);
        return "redirect:/holdings";
    }
}
