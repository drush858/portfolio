package dr.portfolio.dto;

import java.util.List;

public record ImportResult(
        int importedCount,
        List<String> errors
) {}
