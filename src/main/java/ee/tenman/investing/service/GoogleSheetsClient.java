package ee.tenman.investing.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

import static ee.tenman.investing.service.GoogleService.SPREAD_SHEET_ID;

@Service
public class GoogleSheetsClient {

    @Async
    public void update(Sheets sheetsService, String updateCell, Object requestBody) throws IOException {
        ValueRange value = new ValueRange()
                .setValues(Arrays.asList(Arrays.asList(requestBody)));
        sheetsService.spreadsheets().values().update(SPREAD_SHEET_ID, updateCell, value)
                .setValueInputOption("RAW")
                .execute();
    }

}
