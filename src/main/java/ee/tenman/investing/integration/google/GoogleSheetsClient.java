package ee.tenman.investing.integration.google;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;

import static ee.tenman.investing.integration.google.GoogleSheetsService.SPREAD_SHEET_ID;

@Slf4j
@Service
public class GoogleSheetsClient {

    @Resource
    private Sheets googleSheetsApiClient;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    public void update(String updateCell, Object requestBody) throws IOException {
        ValueRange value = new ValueRange()
                .setValues(Arrays.asList(Arrays.asList(requestBody)));
        UpdateValuesResponse response = googleSheetsApiClient.spreadsheets().values().update(SPREAD_SHEET_ID, updateCell, value)
                .setValueInputOption("RAW")
                .execute();
        log.info("{}", response);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
    public void update(BatchUpdateSpreadsheetRequest batchRequest) {
        try {
            BatchUpdateSpreadsheetResponse response = googleSheetsApiClient.spreadsheets()
                    .batchUpdate(SPREAD_SHEET_ID, batchRequest)
                    .execute();
            log.info("{}", response);
        } catch (IOException e) {
            log.error("Failed to update batch request ", e);
        }
    }


    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
    public void update(BatchUpdateSpreadsheetRequest batchRequest, String spreadSheetId) {
        try {
            BatchUpdateSpreadsheetResponse response = googleSheetsApiClient.spreadsheets()
                    .batchUpdate(spreadSheetId, batchRequest)
                    .execute();
            log.info("{}", response);
        } catch (IOException e) {
            log.error("Failed to update batch request ", e);
        }
    }

    public Sheets get() {
        return googleSheetsApiClient;
    }

}
