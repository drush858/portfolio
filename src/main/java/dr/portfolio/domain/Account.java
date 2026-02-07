package dr.portfolio.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.*;

@Table(
	uniqueConstraints = @UniqueConstraint(
		columnNames = {"number", "userId"}
	)
)
@Entity
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "userId", nullable = false)
	private User user;
	
	@Column(nullable = false)
	private String name;
	
	@Column(nullable = false)
	private String number;
	
	@OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
	private List<Holding> holdings = new ArrayList<>();
	
	private Boolean is_active;

	public Account() {}
	
	public List<Holding> getHoldings() {
		return holdings;
	}

	public void setHoldings(List<Holding> holdings) {
		this.holdings = holdings;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getActive() {
		return is_active;
	}

	public void setActive(Boolean is_active) {
		this.is_active = is_active;
	}

	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof Account)) return false;
	    Account account = (Account) o;
	    return id != null && id.equals(account.id);
	}

	@Override
	public int hashCode() {
	    return getClass().hashCode();
	}

}
