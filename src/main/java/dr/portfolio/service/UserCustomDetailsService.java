package dr.portfolio.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dr.portfolio.domain.User;
import dr.portfolio.repositories.UserRepository;

@Service
public class UserCustomDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public UserCustomDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;	
	}

	@Override
	public UserDetails loadUserByUsername(String username)
		throws UsernameNotFoundException {

		User user = userRepository.findByUsername(username);
				//.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return org.springframework.security.core.userdetails.User
				.withUsername(user.getUsername())
				.password(user.getPassword())
				.roles("USER")
				.build();
	    }
	}
