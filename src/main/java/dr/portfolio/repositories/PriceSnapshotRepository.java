package dr.portfolio.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dr.portfolio.domain.PriceSnapshot;

public interface PriceSnapshotRepository
        extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findBySymbol(String symbol);
}
