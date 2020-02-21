package ee.tenman.investing.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.abs;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.joining;

@Component
public class GoogleService {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleService.class);
    private static final String SPREAD_SHEET_ID = "1Buo5586QNMC6v40C0bbD2MTH673dWN12FTgn_oAfIsM";
    private static final String RANGE = "investing!B1:C3";
    private static final String VALUE_RENDER_OPTION = "UNFORMATTED_VALUE";
    private static final String DATE_TIME_RENDER_OPTION = "SERIAL_NUMBER";
    private static final NumberFormat DATE_TIME_FORMAT = new NumberFormat()
            .setType("DATE_TIME")
            .setPattern("dd.mm.yyyy h:mm:ss");
    private Sheets sheetsService;

    @Value("private_key.txt")
    ClassPathResource privateKey;

    @Value("private_key_id.txt")
    ClassPathResource privateKeyId;

//    @Scheduled(cron = "0 0/5 * * * *")
    @Scheduled(cron = "0/5 * * * * *")
    public void run() {

        try {
            sheetsService = createSheetsService();
            Spreadsheet spreadsheetResponse = getSpreadSheetResponse();
            ValueRange investingResponse = getValueRange();

            // third request
            Integer sheetID = spreadsheetResponse.getSheets().get(1).getProperties().getSheetId();
            BigDecimal annualReturn = (BigDecimal) investingResponse.getValues().get(0).get(0);
            BigDecimal profit = (BigDecimal) investingResponse.getValues().get(1).get(0);
            BigDecimal totalSavingsAmount =  (BigDecimal) investingResponse.getValues().get(2).get(0);
            BigDecimal currentTimeFromSpreadSheet = (BigDecimal) investingResponse.getValues().get(2).get(1);
            Instant timeFromInvesting =  Instant.ofEpochSecond(
                    currentTimeFromSpreadSheet.subtract(BigDecimal.valueOf(25569))
                            .multiply(BigDecimal.valueOf(24))
                            .multiply(BigDecimal.valueOf(60))
                            .multiply(BigDecimal.valueOf(60)).longValue())
                    .minus(2, HOURS);

            List<RowData> rowData = new ArrayList<>();
            List<CellData> cellData = new ArrayList<>();

            CellData annualReturnCell = new CellData();
            annualReturnCell.setUserEnteredValue(new ExtendedValue().setNumberValue(annualReturn.doubleValue()));
            annualReturnCell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("PERCENT")));
            cellData.add(annualReturnCell);

            CellData profitCell = new CellData();
            profitCell.setUserEnteredValue(new ExtendedValue().setNumberValue(profit.doubleValue()));
            cellData.add(profitCell);

            CellData areaField = new CellData();
            areaField.setUserEnteredValue(new ExtendedValue().setNumberValue(totalSavingsAmount.doubleValue()));
            cellData.add(areaField);

            CellData currentTimeFromSpreadSheetCell = new CellData();
            currentTimeFromSpreadSheetCell.setUserEnteredValue(new ExtendedValue().setNumberValue(currentTimeFromSpreadSheet.doubleValue()));
            currentTimeFromSpreadSheetCell.setUserEnteredFormat(new CellFormat().setNumberFormat(DATE_TIME_FORMAT));
            cellData.add(currentTimeFromSpreadSheetCell);

            Instant now = Instant.now();
            CellData machineDateTimeCell = new CellData();
            machineDateTimeCell.setUserEnteredValue(new ExtendedValue().setNumberValue(now.getEpochSecond()/86400.0+25569));
            machineDateTimeCell.setUserEnteredFormat(new CellFormat().setNumberFormat(DATE_TIME_FORMAT));
            cellData.add(machineDateTimeCell);

            CellData updateDifferenceInSeconds = new CellData();
            long duration = abs(between(now, timeFromInvesting).get(SECONDS));
            updateDifferenceInSeconds.setUserEnteredValue(new ExtendedValue().setNumberValue((double) duration));
            cellData.add(updateDifferenceInSeconds);

            rowData.add(new RowData().setValues(cellData));

            AppendCellsRequest appendCellRequest = new AppendCellsRequest();
            appendCellRequest.setSheetId(sheetID);
            appendCellRequest.setRows(rowData);
            appendCellRequest.setFields("userEnteredValue,userEnteredFormat.numberFormat");

            List<Request> requests = new ArrayList<>();
            requests.add(new Request().setAppendCells(appendCellRequest));
            BatchUpdateSpreadsheetRequest batchRequests = new BatchUpdateSpreadsheetRequest();
            batchRequests.setRequests(requests);

            BatchUpdateSpreadsheetResponse response = sheetsService.spreadsheets()
                    .batchUpdate(SPREAD_SHEET_ID, batchRequests)
                    .execute();

            LOG.info("{}", response);
        } catch (Exception e){
            LOG.error("Error ", e);
        }
    }

    private ValueRange getValueRange() throws IOException {
        Sheets.Spreadsheets.Values.Get getInvestingRequest =
                sheetsService.spreadsheets().values().get(SPREAD_SHEET_ID, RANGE);
        getInvestingRequest.setValueRenderOption(VALUE_RENDER_OPTION);
        getInvestingRequest.setDateTimeRenderOption(DATE_TIME_RENDER_OPTION);

        return getInvestingRequest.execute();
    }

    public Spreadsheet getSpreadSheetResponse() throws IOException {
        boolean includeGridData = false;
        Sheets.Spreadsheets.Get spreadsheetRequest = sheetsService.spreadsheets().get(SPREAD_SHEET_ID);
        spreadsheetRequest.setRanges(new ArrayList<>());
        spreadsheetRequest.setIncludeGridData(includeGridData);

        return spreadsheetRequest.execute();
    }

    public Sheets createSheetsService() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        Credential httpRequestInitializer = authorizeWithServiceAccount();
        return new Sheets.Builder(httpTransport, jsonFactory, httpRequestInitializer)
                .setApplicationName("Google-SheetsSample/0.1")
                .build();
    }

    private Credential authorizeWithServiceAccount() throws GeneralSecurityException, IOException {

        return new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setServiceAccountId( "splendid-myth-268820@appspot.gserviceaccount.com" )
                .setServiceAccountScopes( Collections.singletonList(SheetsScopes.SPREADSHEETS) )
                .setServiceAccountPrivateKeyId(getPrivateKeyId())
                .setServiceAccountPrivateKey(buildPrivateKey())
                .build();
    }

    private String getPrivateKeyId() {

        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(privateKeyId.getInputStream()))) {
            return buffer.lines().collect(joining(""));
        } catch (IOException e) {
            LOG.error("getPrivateKeyId ", e);
            return null;
        }
    }

    public PrivateKey buildPrivateKey() {
        try {
            // Read in the key into a String
            StringBuilder pkcs8Lines = new StringBuilder();
            InputStream resource = privateKey.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource));
            String line;
            while ((line = reader.readLine()) != null) {
                pkcs8Lines.append(line);
            }

            // Remove the "BEGIN" and "END" lines, as well as any whitespace
            String pkcs8Pem = pkcs8Lines.toString();
            pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");

            // Base64 decode the result
            byte[] pkcs8EncodedBytes = Base64.decodeBase64(pkcs8Pem.getBytes());

            // extract the private key
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privKey = kf.generatePrivate(keySpec);
            System.out.println(privKey);
            return privKey;
        } catch (Exception e){
            return null;
        }
    }

}
