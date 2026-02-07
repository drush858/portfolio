package dr.portfolio.service;

import java.io.PrintWriter;
import java.util.List;

import org.springframework.stereotype.Service;

import dr.portfolio.dto.AccountSoldTotals;
import dr.portfolio.dto.AccountSymbolTotals;
import dr.portfolio.dto.SoldHoldingView;

@Service
public class SoldHoldingsExportService {

    public void writeCsv(
            PrintWriter writer,
            List<SoldHoldingView> holdings,
            List<AccountSymbolTotals> symbolTotals,
            List<AccountSoldTotals> accountTotals
    ) {

        writer.println("Account,Symbol,Quantity Sold,Proceeds,Cost Basis,Gain");

        for (SoldHoldingView v : holdings) {
            writer.printf(
                "%s,%s,%d,%.2f,%.2f,%.2f%n",
                v.getAccountName(),
                v.getSymbol(),
                v.getQuantitySold(),
                v.getProceeds(),
                v.getCostBasis(),
                v.getGain()
            );
        }

        writer.println();
        writer.println("Account Totals");
        writer.println("Account,Proceeds,Cost Basis,Gain");

        for (AccountSoldTotals t : accountTotals) {
            writer.printf(
                "%s,%.2f,%.2f,%.2f%n",
                t.getAccountName(),
                t.getTotalProceeds(),
                t.getTotalCostBasis(),
                t.getTotalGain()
            );
        }
    }
}

