package dr.portfolio.service;

import java.util.Comparator;
import java.util.List;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dr.portfolio.domain.Account;
import dr.portfolio.repositories.AccountRepository;

@Service
@Transactional
public class AccountService {

	private final AccountRepository accountRepository;

    public AccountService(
    		AccountRepository accountRepository) {
    	
        this.accountRepository = accountRepository;
    }
	
    public Account findByAccountIdAndUsername(UUID id, String username) {
    
    	return accountRepository
    		.findByIdAndUserUsername(id, username)
    		.orElseThrow(() ->
        	new IllegalArgumentException(
        		"Account ID not found for user"
            )
        );
    }
    
    public Account findByNumberAndUsername(String accountNumber, String username) {
    
    	return accountRepository
    		.findByNumberAndUserUsername(accountNumber, username)
            .orElseThrow(() ->
            	new IllegalArgumentException(
            		"Account " + accountNumber + " not found for user"
                )
            );
    }
    
    public List<Account> findByUserUsername(String username) {
    	
    	List<Account> accts = accountRepository.findByUserUsername(username);
    	accts.sort(Comparator.comparing(Account::getName, String.CASE_INSENSITIVE_ORDER));
    	return accts;
    }
    
    public List<Account> getAll(UUID userid) {
    	
    	return accountRepository.findByUser_Id(userid);
    }
    
    public List<Account> getAccountsForUser(String username) {
        return accountRepository.findByUserUsername(username);
    }

    public Account save(Account acct) {
    	
    	String name = acct.getName();
    	String number = acct.getNumber();
    	
    	if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Please enter a name.");
        }
    	if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Please enter a number.");
        }
	 
    	return accountRepository.save(acct);
    }
    
    public void deleteById(UUID userid) {
    	
    	accountRepository.deleteById(userid);
    }
	
}
