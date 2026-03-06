package dr.portfolio.service;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import dr.portfolio.dto.AccountSoldTotals;
import dr.portfolio.dto.AccountSymbolTotals;
import dr.portfolio.dto.SoldHoldingView;

@Service
public class SoldHoldingsExcelExportService {

    public Workbook createWorkbook(
            List<SoldHoldingView> holdings,
            List<AccountSymbolTotals> symbolTotals,
            List<AccountSoldTotals> accountTotals
    ) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sold Holdings");

        int rowNum = 0;

        Row header = sheet.createRow(rowNum++);
        header.createCell(0).setCellValue("Account");
        header.createCell(1).setCellValue("Symbol");
        header.createCell(2).setCellValue("Qty Sold");
        header.createCell(3).setCellValue("Proceeds");
        header.createCell(4).setCellValue("Cost Basis");
        header.createCell(5).setCellValue("Gain");
        header.createCell(6).setCellValue("Dividends");
        header.createCell(7).setCellValue("TotalReturn");
        header.createCell(8).setCellValue("TotalReturn%");
        
        for (SoldHoldingView v : holdings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(v.getAccountName());
            row.createCell(1).setCellValue(v.getSymbol());
            row.createCell(2).setCellValue(v.getQuantitySold());
            row.createCell(3).setCellValue(v.getProceeds());
            row.createCell(4).setCellValue(v.getCostBasis());
            row.createCell(5).setCellValue(v.getGain());
            row.createCell(6).setCellValue(v.getDividends());
            row.createCell(7).setCellValue(v.getTotalReturn());
            row.createCell(8).setCellValue(v.getTotalReturnPercent());
        }

        rowNum++; // spacer

        Row totalsHeader = sheet.createRow(rowNum++);
        totalsHeader.createCell(0).setCellValue("Account Totals");

        for (AccountSoldTotals t : accountTotals) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(t.getAccountName());
            row.createCell(3).setCellValue(t.getTotalProceeds());
            row.createCell(4).setCellValue(t.getTotalCostBasis());
            row.createCell(5).setCellValue(t.getTotalGain());
            row.createCell(6).setCellValue(t.getDividends());
            row.createCell(7).setCellValue(t.getTotalReturn());
            row.createCell(8).setCellValue(t.getTotalReturnPercent());
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }

        return wb;
    }
}

