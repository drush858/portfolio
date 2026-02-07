package dr.portfolio.dto.market;

import java.time.Instant;

public record CachedPrice(
    Double price,
    Instant fetchedAt) 
{
	public Instant updatedAt() {
		return fetchedAt;
	}
}
