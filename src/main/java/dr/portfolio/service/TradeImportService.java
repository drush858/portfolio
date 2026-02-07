package dr.portfolio.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.ImportResult;
import dr.portfolio.dto.TradeCreate;

@Service
@Transactional
public class TradeImportService {

    private final AccountService accountService;
    private final TradeService tradeService;

    public TradeImportService(
            AccountService accountService,
            TradeService tradeService
    ) {
        this.accountService = accountService;
        this.tradeService = tradeService;
    }

    public ImportResult importTrades(
            MultipartFile file,
            String username
    ) throws IOException {

        List<String> errors = new ArrayList<>();
        int imported = 0;

        try (
            InputStreamReader reader = new InputStreamReader(file.getInputStream());
        	
        		/* possible fix for deprecated withFirstRecordAsHeader warnings
        		 * 
        		CSVFormat format = CSVFormat.DEFAULT.builder()
        				.setHeader() // Automatically uses the first record as header
        				.setSkipHeaderRecord(true) // Skips the header row during parsing
        				.build();
        		CSVParser parser = new CSVParser(reader, format);
        		*/
        		
            CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withTrim()
                .parse(reader)
        ) {
            for (CSVRecord r : parser) {
                try {
                    TradeCreate trade = mapRow(r, username);
                    importTrade(trade, username);
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + r.getRecordNumber() + ": " + e.getMessage());
                }
            }
        }

        return new ImportResult(imported, errors);
    }

    private TradeCreate mapRow(
            CSVRecord r,
            String username
    ) {

        Account account =
            accountService.findByNumberAndUsername(
                r.get("accountNumber"),
                username
            );

        TradeCreate trade = new TradeCreate();
        trade.setAccountId(account.getId());
        trade.setSymbol(r.get("symbol").toUpperCase());
        trade.setQuantity(Integer.parseInt(r.get("quantity")));
        trade.setPrice(Double.parseDouble(r.get("price")));
      
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate parsedDate = LocalDate.parse(r.get("tradeDate"), formatter);
         
        trade.setDate(parsedDate);

        TradeType type =
            TradeType.valueOf(r.get("type").toUpperCase());

        trade.setTradeType(type);
        return trade;
    }

    private void importTrade(TradeCreate trade, String username) throws ParseException {
        if (trade.getTradeType() == TradeType.BUY) {
            tradeService.buy(trade, username);
        } else {
            tradeService.sell(trade, username);
        }
    }
}
