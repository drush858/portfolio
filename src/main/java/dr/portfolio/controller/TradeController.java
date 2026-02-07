package dr.portfolio.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import dr.portfolio.domain.OptionType;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.ImportResult;
import dr.portfolio.dto.SoldHoldingsResult;
import dr.portfolio.dto.TradeCreate;
import dr.portfolio.service.OptionTradeService;
import dr.portfolio.service.RealizedGainService;
import dr.portfolio.service.SoldHoldingsExcelExportService;
import dr.portfolio.service.SoldHoldingsExportService;
import dr.portfolio.service.TradeImportService;
import dr.portfolio.service.TradeService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/trades")
public class TradeController {

    private final TradeService tradeService;
    private final RealizedGainService realizedGainService;
    private final SoldHoldingsExportService exportService;
    private final SoldHoldingsExcelExportService excelExportService;
    private final TradeImportService tradeImportService;
    private final OptionTradeService optionTradeService;

    public TradeController(TradeService tradeService, 
    		               RealizedGainService realizedGainService,
    		               SoldHoldingsExportService exportService,
    		               SoldHoldingsExcelExportService excelExportService,
    		               TradeImportService tradeImportService,
    		               OptionTradeService optionTradeService) {
    	
        this.tradeService = tradeService;
        this.realizedGainService = realizedGainService;
        this.exportService = exportService;
        this.excelExportService = excelExportService;
        this.tradeImportService = tradeImportService;
        this.optionTradeService = optionTradeService;
    }

    @GetMapping("/sold/{year}")
    public String soldHoldings(
            @PathVariable int year,
            Principal principal,
            Model model) {

        List<Trade> trades =
        	    tradeService.getTradesForUser(principal.getName());

        SoldHoldingsResult result =
                realizedGainService.getSoldHoldingsForYear(trades, year);

            model.addAttribute("soldHoldings", result.soldHoldings());
            model.addAttribute("accountTotals", result.accountTotals());
            model.addAttribute("accountSymbolTotals", result.accountSymbolTotals());
            model.addAttribute("year", year);
            model.addAttribute("pageTitle", "Sold Holdings");

        return "soldHoldings";
    }
    
    @GetMapping("/sold/{year}/export/csv")
    public void exportSoldCsv(
            @PathVariable int year,
            Principal principal,
            HttpServletResponse response
    ) throws IOException {

        List<Trade> trades =
            tradeService.getTradesForUser(principal.getName());

        SoldHoldingsResult result =
            realizedGainService.getSoldHoldingsForYear(trades, year);

        response.setContentType("text/csv");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"sold-holdings-" + year + ".csv\""
        );

        try (PrintWriter writer = response.getWriter()) {
            exportService.writeCsv(
                writer,
                result.soldHoldings(),
                result.accountSymbolTotals(),
                result.accountTotals()
            );
        }
    }

    @GetMapping("/sold/{year}/export/excel")
    public void exportSoldExcel(
            @PathVariable int year,
            Principal principal,
            HttpServletResponse response
    ) throws IOException {

        List<Trade> trades =
            tradeService.getTradesForUser(principal.getName());

        SoldHoldingsResult result =
            realizedGainService.getSoldHoldingsForYear(trades, year);

        response.setContentType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=\"sold-holdings-" + year + ".xlsx\""
        );

        Workbook wb = excelExportService.createWorkbook(
            result.soldHoldings(),
            result.accountSymbolTotals(),
            result.accountTotals()
        );

        wb.write(response.getOutputStream());
        wb.close();
    }

    @PostMapping("/import")
    public String importTrades(
            @RequestParam("file") MultipartFile file,
            Principal principal,
            Model model
    ) throws IOException {

        ImportResult result =
            tradeImportService.importTrades(
                file,
                principal.getName()
            );

        model.addAttribute("imported", result.importedCount());
        model.addAttribute("errors", result.errors());

        return "trade-import-result";
    }
    
    @PostMapping("/buy/{id}")
    public String buy(
    		@ModelAttribute TradeCreate trade, 
    		@PathVariable UUID id,
    		Principal principal) {
    	
    	trade.setAccountId(id);
    	try {
    		tradeService.buy(trade, principal.getName());
    		
    	} catch (Exception p) {
    		
    		throw new IllegalArgumentException("Invalid trade date", p);
    	}
        return "redirect:/holdings/view/{id}";
    }
    
    @PostMapping("/stock/add")
    public String addStockTrade(
            @RequestParam UUID accountId,
            @RequestParam String symbol,
            @RequestParam int quantity,
            @RequestParam double price,
            @RequestParam LocalDate tradeDate,
            Principal principal
    ) {

    	TradeCreate trade = new TradeCreate();
    	trade.setAccountId(accountId);
    	trade.setSymbol(symbol);
    	trade.setQuantity(quantity);
    	trade.setPrice(price);
    	trade.setDate(tradeDate);
    	trade.setTradeType(TradeType.BUY);
    	try {
    		tradeService.buy(trade, principal.getName());
    		
    	} catch (Exception p) {
    		
    		throw new IllegalArgumentException("Invalid Args", p);
    	}
    	return "redirect:/holdings/view/" + accountId;
    }
    
    @PostMapping("/sell/{id}")
    public String sell(
    		@ModelAttribute TradeCreate trade, 
    		@PathVariable UUID id,
    		Principal principal) {
    	
    	trade.setAccountId(id);
    	try {
    		tradeService.sell(trade, principal.getName());
    		
    	} catch (Exception p) {
    		
    		throw new IllegalArgumentException("Invalid trade date", p);
    	}
        return "redirect:/holdings/view/{id}";
    }
    
    @PostMapping("/stock/sell")
    public String sellStockTrade(
            @RequestParam UUID accountId,
            @RequestParam String symbol,
            @RequestParam int quantity,
            @RequestParam double price,
            @RequestParam LocalDate tradeDate,
            Principal principal
    ) {

    	TradeCreate trade = new TradeCreate();
    	trade.setAccountId(accountId);
    	trade.setSymbol(symbol);
    	trade.setQuantity(quantity);
    	trade.setPrice(price);
    	trade.setDate(tradeDate);
    	trade.setTradeType(TradeType.SELL);
    	try {
    		tradeService.sell(trade, principal.getName());
    		
    	} catch (Exception p) {
    		
    		throw new IllegalArgumentException("Invalid Args", p);
    	}
    	return "redirect:/holdings/view/" + accountId;
    }

    @PostMapping("/delete/{id}")
    public String delete(
    		@PathVariable UUID id,
    		Principal principal) {
    	
    	tradeService.deleteById(id, principal.getName());
    	
        return "redirect:/trades";
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<String> getTrade(@PathVariable UUID id) {
        
    	
    	if (id == null) {
    		return ResponseEntity.notFound().build();
    	}
    	
    	Trade trade = tradeService.getTrade(id);
    	
    	if (trade != null) {

    		return ResponseEntity.ok("<hr> Account: " + trade.getHolding().getAccount().getName()
    			+ "<hr> Stock: " + trade.getSymbol()
    			+ "<hr> Qty: " + trade.getQuantity() 
    			+ "<hr> Price: " + trade.getPrice());
    	} else {
    		
    		 return ResponseEntity.notFound().build();
    	}
    }
    
    @GetMapping("/lots/{id}")
    public String getTradeLots(@PathVariable UUID id, Principal principal, Model model) {
        
    	List<Trade> trades = tradeService.getTradesForHolding(id, principal.getName());

        model.addAttribute("trades", trades);

        return "fragments/trade-lots :: tradeLots";     
    }
    
    @PostMapping("/option/add")
    public String addOptionTrade(
            @RequestParam UUID accountId,

            @RequestParam String underlyingSymbol,
            @RequestParam OptionType optionType,
            @RequestParam LocalDate expiration,
            @RequestParam BigDecimal strikePrice,

            @RequestParam int quantity,
            @RequestParam BigDecimal price,
            @RequestParam TradeType tradeType,

            @RequestParam LocalDate tradeDate
    ) {

        optionTradeService.recordOptionTrade(
                accountId,
                underlyingSymbol.trim().toUpperCase(),
                optionType,
                expiration,
                strikePrice,
                quantity,
                price,
                tradeType,
                tradeDate.atStartOfDay()
        );

        return "redirect:/holdings/view/" + accountId;
    }
    
}

