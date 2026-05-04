package dr.portfolio.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.OptionType;
import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.ImportResult;
import dr.portfolio.dto.SoldHoldingsResult;
import dr.portfolio.dto.TradeCreate;
import dr.portfolio.dto.TradeEditForm;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.service.OptionTradeService;
import dr.portfolio.service.RealizedGainService;
import dr.portfolio.service.SoldHoldingsExcelExportService;
import dr.portfolio.service.SoldHoldingsExportService;
import dr.portfolio.service.TradeImportService;
import dr.portfolio.service.TradeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/trades")
public class TradeController {

    private final TradeService tradeService;
    private final RealizedGainService realizedGainService;
    private final SoldHoldingsExportService exportService;
    private final SoldHoldingsExcelExportService excelExportService;
    private final TradeImportService tradeImportService;
    private final OptionTradeService optionTradeService;
    private final AccountRepository accountRepository;

    public TradeController(TradeService tradeService, 
    		               RealizedGainService realizedGainService,
    		               SoldHoldingsExportService exportService,
    		               SoldHoldingsExcelExportService excelExportService,
    		               TradeImportService tradeImportService,
    		               OptionTradeService optionTradeService,
    		               AccountRepository accountRepository) {
    	
        this.tradeService = tradeService;
        this.realizedGainService = realizedGainService;
        this.exportService = exportService;
        this.excelExportService = excelExportService;
        this.tradeImportService = tradeImportService;
        this.optionTradeService = optionTradeService;
        this.accountRepository = accountRepository;
    }
    
    private boolean isOptionSymbol(String symbol) {
        return symbol != null && symbol.matches(".*\\d{6}[CP].*");
    }
    
    @GetMapping("/view/{id}")
    public String listTrades(@PathVariable UUID id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String symbol,
                             Model model,
                             Principal principal) {

        Account account = accountRepository.getReferenceById(id);

        Page<Trade> tradePage = tradeService.getTradesForAccount(
        		id, page, size, symbol, principal.getName());

        int start = tradePage.getTotalElements() == 0 ? 0 : page * size + 1;
        int end = Math.min((page + 1) * size, (int) tradePage.getTotalElements());

        model.addAttribute("tradePage", tradePage);
        model.addAttribute("page", tradePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("symbol", symbol);

        model.addAttribute("pageTitle", account.getName() + " Trades");
        model.addAttribute("account", account);

        return "trades";
    }

    @GetMapping("/options/{id}/edit")
    public String showEditOptionForm(@PathVariable UUID id,
                                    Model model,
                                    Principal principal) throws AccessDeniedException {

        Trade trade = tradeService.getTradeForUser(id, principal.getName());

        if (!isOptionSymbol(trade.getSymbol())) {
            throw new IllegalArgumentException("Not an option trade");
        }

        TradeEditForm form = new TradeEditForm();
        form.setTradeId(trade.getId());
        form.setAccountId(trade.getHolding().getAccount().getId());
        form.setSymbol(trade.getSymbol());
        form.setQuantity(trade.getQuantity());
        form.setPrice(trade.getPrice());
        form.setTradeDate(trade.getTradeDate());

        model.addAttribute("tradeEditForm", form);
        model.addAttribute("trade", trade);

        return "trade-edit-option";
    }
    
    @PostMapping("/options/{id}/edit")
    public String updateOption(@PathVariable UUID id,
                               @ModelAttribute TradeEditForm form,
                               Principal principal,
                               RedirectAttributes redirectAttributes) throws AccessDeniedException {

        form.setTradeId(id);

        tradeService.updateOptionTrade(form, principal.getName());

        redirectAttributes.addFlashAttribute("successMessage", "Option trade updated");

        return "redirect:/trades/view/" + form.getAccountId();
    }
    
    
    @GetMapping("/{id}/edit")
    public String showEditTradeForm(@PathVariable UUID id,
                                   Model model,
                                   Principal principal) throws AccessDeniedException {

        Trade trade = tradeService.getTradeForUser(id, principal.getName());

        TradeEditForm form = new TradeEditForm();
        form.setTradeId(trade.getId());
        form.setAccountId(trade.getHolding().getAccount().getId());
        form.setSymbol(trade.getSymbol());
        form.setQuantity(trade.getQuantity());
        form.setPrice(trade.getPrice());
        form.setTradeDate(trade.getTradeDate());

        model.addAttribute("tradeEditForm", form);
        model.addAttribute("trade", trade);

        return "trade-edit";
    }
    
    @PostMapping("/{id}/edit")
    public String updateTrade(@PathVariable UUID id,
                              @ModelAttribute TradeEditForm form,
                              Principal principal,
                              RedirectAttributes redirectAttributes) throws AccessDeniedException {

        form.setTradeId(id);

        tradeService.updateTrade(form, principal.getName());

        redirectAttributes.addFlashAttribute("successMessage", "Trade updated");

        return "redirect:/trades/view/" + form.getAccountId();
    }
    
    
    @GetMapping("/sell/{id}/edit")
    public String showEditSellForm(
    		@PathVariable UUID id, 
    		Model model, Principal principal) throws AccessDeniedException {
    	
        Trade trade = tradeService.getTradeForUser(id, principal.getName());

        if (trade.getTradeType() != TradeType.SELL) {
            throw new IllegalArgumentException("Not a sell trade");
        }

        TradeEditForm form = new TradeEditForm();
        form.setAccountId(trade.getHolding().getAccount().getId());
        form.setSymbol(trade.getSymbol());
        form.setTradeId(trade.getId());
        form.setTradeDate(trade.getTradeDate());
        form.setPrice(trade.getPrice());
        form.setQuantity(trade.getQuantity());

        model.addAttribute("tradeEditForm", form);
        model.addAttribute("trade", trade);

        return "trade-edit-sell";
    }
    
    @PostMapping("/sell/{id}/edit")
    public String updateSell(@PathVariable UUID id,
                            @Valid @ModelAttribute TradeEditForm tradeEditForm,
                            BindingResult bindingResult,
                             Principal principal,
                             RedirectAttributes redirectAttributes) throws AccessDeniedException {
    	
		if (bindingResult.hasErrors()) {
			return "trade-edit-sell";
		}
    	
        tradeEditForm.setTradeId(id);
        tradeService.updateSellTrade(tradeEditForm, principal.getName());

        redirectAttributes.addFlashAttribute("successMessage", "Sell trade updated");
        return "redirect:/trades{id}";
    }
    
    
    /*
    @PostMapping("/buy/{id}")
    public String buy(
    		@PathVariable UUID id,
    		@Valid @ModelAttribute("tradeCreate") TradeCreate trade, 
    		BindingResult bindingResult,
    		Principal principal,
    		Model model
    ) {
    
    	if (bindingResult.hasErrors()) {
            // re-hydrate page data your holdings page needs
            //var page = holdingsPageService.buildHoldingsPage(id, principal.getName());
            model.addAttribute("accountId", id);
            //model.addAttribute("accountName", page.accountName());
            //model.addAttribute("holdings", page.holdings());
            // keep tradeCreate (already in model) + errors
            model.addAttribute("showTradeModal", true); // so modal opens with errors
            return "redirect:/holdings/view/{id}";
        }
    	
    	trade.setAccountId(id);
    	try {
    		tradeService.buy(trade, principal.getName());
    		
    	} catch (Exception p) {
    		
    		throw new IllegalArgumentException("Invalid trade date", p);
    	}
        return "redirect:/holdings/view/{id}";
    }*/
    
    @PostMapping("/stock/add")
    public String addStockTrade(
            @RequestParam UUID accountId,
            @RequestParam String symbol,
            @RequestParam int quantity,
            @RequestParam double price,
            @RequestParam LocalDateTime tradeDate,
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
    /*
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
    */
    @PostMapping("/stock/sell")
    public String sellStockTrade(
            @RequestParam UUID accountId,
            @RequestParam String symbol,
            @RequestParam int quantity,
            @RequestParam double price,
            @RequestParam LocalDateTime tradeDate,
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

    @PostMapping("/{id}/delete")
    public String deleteTrade(@PathVariable UUID id,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
    	
        UUID accountId = tradeService.deleteById(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Trade deleted");
        return "redirect:/trades/view/" + accountId;
    }
    
    @GetMapping("/{id}&{accountId}")
    public String showTrade(@PathVariable UUID id,
    						@PathVariable UUID accountId,
                            Model model,
                            Principal principal) throws AccessDeniedException {

    	Trade trade = tradeService.getTradeForUser(id, principal.getName());
    	CashTransaction cashTransaction = tradeService.getCashTransactionForTrade(id, principal.getName());

    	model.addAttribute("trade", trade);
    	model.addAttribute("cashTransaction", cashTransaction);
    	model.addAttribute("accountId", accountId);
    	model.addAttribute("pageTitle", "Trade Detail");

        return "trade-view";
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

            @RequestParam LocalDateTime tradeDate
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
                tradeDate
        );

        return "redirect:/holdings/view/" + accountId;
    }
    
    @GetMapping("/sold")
    public String soldHoldings(
            @RequestParam int year,
    		Principal principal,
            Model model) {

        List<Trade> trades =
        	    tradeService.getTradesForUser(principal.getName());

        SoldHoldingsResult result =
                realizedGainService.getSoldHoldingsForYear(trades, year);

        model.addAttribute("soldLotDetails", result.soldLotDetails());
        model.addAttribute("soldHoldings", result.soldHoldings());
        model.addAttribute("accountTotals", result.accountTotals());
        model.addAttribute("accountSymbolTotals", result.accountSymbolTotals());
        model.addAttribute("year", year);
        model.addAttribute("pageTitle", "Sold Holdings");

        return "soldHoldings";
    }
    
    @GetMapping("/sold/export/csv")
    public void exportSoldCsv(
            @RequestParam int year,
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

    @GetMapping("/sold/export/excel")
    public void exportSoldExcel(
            @RequestParam int year,
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
    
}

