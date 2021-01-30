package ee.tenman.investing.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

import static ee.tenman.investing.service.GoogleService.SPREAD_SHEET_ID;

@Service
public class GoogleSheetsClient {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSheetsClient.class);

    @Async
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    public void update(Sheets sheetsService, String updateCell, Object requestBody) throws IOException {
        ValueRange value = new ValueRange()
                .setValues(Arrays.asList(Arrays.asList(requestBody)));
        UpdateValuesResponse response = sheetsService.spreadsheets().values().update(SPREAD_SHEET_ID, updateCell, value)
                .setValueInputOption("RAW")
                .execute();
        LOG.info("{}", response);
    }

    @Async
    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
    public void update(Sheets sheetsService, BatchUpdateSpreadsheetRequest batchRequest) {
        try {
            BatchUpdateSpreadsheetResponse response = sheetsService.spreadsheets()
                    .batchUpdate(SPREAD_SHEET_ID, batchRequest)
                    .execute();
            LOG.info("{}", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
