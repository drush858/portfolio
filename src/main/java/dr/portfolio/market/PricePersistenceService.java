package dr.portfolio.market;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.PriceSnapshot;
import dr.portfolio.repositories.PriceSnapshotRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;

@Service
public class PricePersistenceService {

    private final PriceCache priceCache;
    private final PriceSnapshotRepository repository;

    public PricePersistenceService(
            PriceCache priceCache,
            PriceSnapshotRepository repository) {
        this.priceCache = priceCache;
        this.repository = repository;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("PricePersistenceService initialized");
    }


    @PreDestroy
    @Transactional
    public void persistPrices() {

    	System.out.println("Persisting price cache to DB…");
    	
        priceCache.snapshot().forEach((symbol, cached) -> {

            PriceSnapshot snap = repository
                .findBySymbol(symbol)
                .orElseGet(() ->
                    new PriceSnapshot(
                        symbol,
                        cached.price(),
                        cached.updatedAt()
                    )
                );

            snap.setPrice(cached.price());
            snap.setUpdatedAt(cached.updatedAt());

            repository.save(snap);
        });
    }
}

