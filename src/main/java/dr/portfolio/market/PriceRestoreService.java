package dr.portfolio.market;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import dr.portfolio.dto.market.CachedPrice;
import dr.portfolio.repositories.PriceSnapshotRepository;

@Service
public class PriceRestoreService {

    private final PriceSnapshotRepository repository;
    private final PriceCache priceCache;

    public PriceRestoreService(
            PriceSnapshotRepository repository,
            PriceCache priceCache) {
        this.repository = repository;
        this.priceCache = priceCache;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restorePrices() {

        repository.findAll().forEach(snap -> {
            priceCache.restore(
                snap.getSymbol(),
                new CachedPrice(
                    snap.getPrice(),
                    snap.getUpdatedAt()
                )
            );
        });
    }
}

