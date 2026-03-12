package dr.portfolio.service;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Holding;
import dr.portfolio.repositories.HoldingRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class HoldingMaintenanceService {

    private final HoldingRepository holdingRepository;
    private final HoldingRebuildService holdingRebuildService;

    public HoldingMaintenanceService(
            HoldingRepository holdingRepository,
            HoldingRebuildService holdingRebuildService
    ) {
        this.holdingRepository = holdingRepository;
        this.holdingRebuildService = holdingRebuildService;
    }

    public void rebuildAllHoldings() {
        for (Holding holding : holdingRepository.findAll()) {
            holdingRebuildService.rebuildHolding(holding);
        }
    }
}