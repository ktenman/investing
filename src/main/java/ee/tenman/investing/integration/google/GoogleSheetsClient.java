package ee.tenman.investing.integration.google;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Service
public class GoogleSheetsClient {

    @Resource
    private Sheets googleSheetsApiClient;

    public static final String VALUE_RENDER_OPTION = "UNFORMATTED_VALUE";
    public static final String DATE_TIME_RENDER_OPTION = "SERIAL_NUMBER";
    public static final String SPREAD_SHEET_ID = "1Buo5586QNMC6v40C0bbD2MTH673dWN12FTgn_oAfIsM";

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void update(String updateCell, Object requestBody) throws IOException {
        ValueRange value = new ValueRange()
                .setValues(Arrays.asList(Arrays.asList(requestBody)));
        UpdateValuesResponse response = googleSheetsApiClient.spreadsheets().values().update(SPREAD_SHEET_ID, updateCell, value)
                .setValueInputOption("RAW")
                .execute();
        log.info("{}", response);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
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


    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
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

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public ValueRange getValueRange(String range) {
        try {
            Sheets.Spreadsheets.Values.Get getInvestingRequest =
                    googleSheetsApiClient.spreadsheets().values().get(SPREAD_SHEET_ID, range);
            getInvestingRequest.setValueRenderOption(VALUE_RENDER_OPTION);
            getInvestingRequest.setDateTimeRenderOption(DATE_TIME_RENDER_OPTION);
            return getInvestingRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Spreadsheet getSpreadSheetResponse() {
        try {
            boolean includeGridData = false;
            Sheets.Spreadsheets.Get spreadsheetRequest = googleSheetsApiClient.spreadsheets().get(SPREAD_SHEET_ID);
            spreadsheetRequest.setRanges(new ArrayList<>());
            spreadsheetRequest.setIncludeGridData(includeGridData);
            return spreadsheetRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
