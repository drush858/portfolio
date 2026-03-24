package dr.portfolio.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.domain.User;
import dr.portfolio.dto.CashLedgerRow;
import dr.portfolio.dto.CashSummary;
import dr.portfolio.repositories.AccountRepository;
import dr.portfolio.repositories.UserRepository;
import dr.portfolio.service.AccountService;
import dr.portfolio.service.CashTransactionService;
import dr.portfolio.service.HoldingMaintenanceService;
import dr.portfolio.service.IncomeDashboardService;

@Controller
@RequestMapping("/accounts")
public class AccountController {

	private final AccountService accountService;
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final CashTransactionService cashTransactionService;
	private final IncomeDashboardService incomeDashboardService;
	private final HoldingMaintenanceService holdingMaintenanceService;
	
    public AccountController(AccountService accountService, 
    						 UserRepository userRepository,
    						 AccountRepository accountRepository,
    						 CashTransactionService cashTransactionService,
    						 IncomeDashboardService incomeDashboardService,
    						 HoldingMaintenanceService holdingMaintenanceService) {
		super();
		this.accountService = accountService;
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.cashTransactionService = cashTransactionService;
		this.incomeDashboardService = incomeDashboardService;
		this.holdingMaintenanceService = holdingMaintenanceService;
	}

    @PostMapping("/admin/rebuild-holdings")
    public String rebuildHoldings() {
        holdingMaintenanceService.rebuildAllHoldings();
        return "redirect:/accounts";
    }
    
    @GetMapping("/income")
    public String incomeDashboard(
            @RequestParam int year,
            Principal principal,
            Model model
    ) {
        var result = incomeDashboardService.buildForUser(principal.getName(), year);

        model.addAttribute("pageTitle", "Income Dashboard");
        model.addAttribute("year", year);
        model.addAttribute("summary", result.summary());
        model.addAttribute("monthlyRows", result.monthlyRows());
        model.addAttribute("symbolRows", result.symbolRows());

        return "income-dashboard";
    }
    
    @GetMapping("/account")
    public String viewAccounts(Model model, Principal principal) {

        String username = principal.getName();
        List<Account> accounts = accountRepository.findByUserUsername(username);
        model.addAttribute("accounts", accounts);
        model.addAttribute("account", new Account());
        return "accounts";
    }

    
	@GetMapping("/all")
    public AccountDto getAll(@PathVariable UUID userid) {
        
    	List<Account> accounts = accountService.getAll(userid);
    	AccountDto acct = new AccountDto(accounts.get(0).getName(), accounts.get(0).getNumber());
    	return acct;
    }
	
	record AccountDto(String name, String number) {}
	
	@GetMapping
    public String list(Model model, Principal principal) {
		String username = principal.getName();

		model.addAttribute("pageTitle", "My Accounts");
        model.addAttribute("accounts", accountService.findByUserUsername(username));
        model.addAttribute("account", new Account());
        return "accounts";
    }
	
	@PostMapping
	public String create(@ModelAttribute Account account, Principal principal) {

		User user = userRepository.findByUsername(principal.getName());
		account.setUser(user);
		account.setActive(true);
		accountService.save(account);
	    return "redirect:/accounts";
	}
	 
	@PostMapping("/deactivate/{id}")
	public String deactivate(@PathVariable UUID id) {

		Account account = accountRepository.getReferenceById(id);
		account.setActive(false);
		accountRepository.save(account);
		return "redirect:/accounts";
	}

	@PostMapping("/activate/{id}")
	public String activate(@PathVariable UUID id) {

		Account account = accountRepository.getReferenceById(id);
		account.setActive(true);
		accountRepository.save(account);
		return "redirect:/accounts";
	}

	@GetMapping("/cash/{id}")
	public String cashLedger( 
			@PathVariable UUID id,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size, 
	        @RequestParam(required = false) String symbol,
	        Principal principal, 
	        Model model) {

		Account account = accountService.findByAccountIdAndUsername(id, principal.getName());

		Page<CashLedgerRow> pageObj =
		        cashTransactionService.buildLedger(id, page, size, symbol);
		CashSummary summary = cashTransactionService.calculateSummary(id);
		List<String> symbols = cashTransactionService.findSymbols(id);

		model.addAttribute("pageTitle", account.getName() + " Cash Ledger");
		model.addAttribute("account", account);
		model.addAttribute("accountId", id);
		model.addAttribute("summary", summary);
		model.addAttribute("symbols", symbols);
		model.addAttribute("transactions", pageObj.getContent());
		model.addAttribute("size", size);
		
		int start = pageObj.getTotalElements() == 0 ? 0 : page * size + 1;
		int end = Math.min((page + 1) * size, (int) pageObj.getTotalElements());

		model.addAttribute("pageObj", pageObj);
		model.addAttribute("size", size);
		model.addAttribute("start", start);
		model.addAttribute("end", end);
		model.addAttribute("symbol", symbol == null ? "" : symbol);

		return "cash-ledger";
	}

	@PostMapping("/cash/dividend")
	public String addDividend(
			@RequestParam UUID accountId, 
			@RequestParam LocalDate transactionDate,
			@RequestParam BigDecimal amount, 
			@RequestParam(required = false) String symbol,
			@RequestParam(required = false) String description) 
	{
		cashTransactionService.addDividend(accountId, transactionDate.atStartOfDay(), amount, symbol, description);
		return "redirect:/accounts/cash/" + accountId;
	}

	@PostMapping("/cash/add")
	public String addCash(
			@RequestParam UUID accountId, 
			@RequestParam LocalDate transactionDate,
			@RequestParam BigDecimal amount, 
			@RequestParam CashTransactionType type,
			@RequestParam(required = false) String description) 
	{
		cashTransactionService.addCash(accountId, transactionDate.atStartOfDay(), amount, type, description);
		return "redirect:/accounts/cash/" + accountId;
	}

	@PostMapping("/cash/withdraw")
	public String withdrawCash(
			@RequestParam UUID accountId, 
			@RequestParam LocalDate transactionDate,
			@RequestParam BigDecimal amount, 
			@RequestParam(required = false) String description) 
	{
		cashTransactionService.withdraw(accountId, transactionDate.atStartOfDay(), amount, description);
		return "redirect:/accounts/cash/" + accountId;
	}

	@PostMapping("/cash/edit")
    public String editSubmit(
    		 @RequestParam UUID accountId,
    	     @RequestParam UUID id,
    	     @RequestParam LocalDateTime transactionDate,
    	     @RequestParam CashTransactionType transactionType,
    	     @RequestParam BigDecimal amount,
    	     @RequestParam(required = false) String symbol,
    	     @RequestParam(required = false) String description
    ) {
		cashTransactionService.update(id, transactionDate, transactionType, amount, symbol, description);
    	return "redirect:/accounts/cash/" + accountId;
    }
}
