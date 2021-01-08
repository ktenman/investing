package ee.tenman.investing.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendCellsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.paukov.combinatorics3.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ee.tenman.investing.service.CoinMarketCapService.BINANCE_COIN_ID;
import static ee.tenman.investing.service.CoinMarketCapService.CRO_ID;
import static java.lang.Math.abs;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.joining;

@Service
public class GoogleService {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleService.class);
    public static final String SPREAD_SHEET_ID = "1Buo5586QNMC6v40C0bbD2MTH673dWN12FTgn_oAfIsM";
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
    private BigDecimal leftOverAmount;
    @Resource
    CoinMarketCapService coinMarketCapService;
    @Resource
    GoogleSheetsClient googleSheetsClient;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    @Scheduled(cron = "0 0/5 * * * *")
    public void run() throws Exception {

        try {
            Spreadsheet spreadsheetResponse = getSpreadSheetResponse();
            ValueRange investingResponse = getValueRange("investing!B1:C3");
            BatchUpdateSpreadsheetRequest batchRequest = buildBatchRequest(spreadsheetResponse, investingResponse);
            googleSheetsClient.update(sheetsService, batchRequest);
        } catch (Exception e) {
            LOG.error("Error ", e);
            throw new Exception(e.getMessage());
        }
    }

    @Scheduled(cron = "10 * * * * *")
    @Scheduled(cron = "40 * * * * *")
    @Retryable(value = {Exception.class}, backoff = @Backoff(delay = 200))
    public void updateSumOfTickers() throws Exception {

        try {
            updateTickerAmounts();
        } catch (Exception e) {
            LOG.error("Error ", e);
            throw new Exception(e.getMessage());
        }
    }

    public void removeCells() {
        try {
            Spreadsheet spreadsheetResponse = getSpreadSheetResponse();

            SheetProperties properties = spreadsheetResponse.getSheets().get(1).getProperties();

            Integer sheetID = properties.getSheetId();

            Sheets.Spreadsheets.Values.Get getInvestingRequest =
                    sheetsService.spreadsheets().values().get(SPREAD_SHEET_ID, properties.getTitle());
            getInvestingRequest.setValueRenderOption(VALUE_RENDER_OPTION);
            getInvestingRequest.setDateTimeRenderOption(DATE_TIME_RENDER_OPTION);

            ValueRange valueRange = getInvestingRequest.execute();

            String maximum = valueRange.getRange().split(":")[1].replaceAll("[^\\d.]", "");
            int setEndIndex = Integer.parseInt(maximum);

            DeleteDimensionRequest deleteDimensionRequest = new DeleteDimensionRequest();
            DimensionRange dimensionRange = new DimensionRange();
            dimensionRange.setSheetId(sheetID);
            dimensionRange.setDimension("ROWS");
            dimensionRange.setStartIndex(1000);
            dimensionRange.setEndIndex(setEndIndex);

            deleteDimensionRequest.setRange(dimensionRange);

            List<Request> requests = new ArrayList<>();
            requests.add(new Request().setDeleteDimension(deleteDimensionRequest));
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

    @Scheduled(cron = "30 * * * * *")
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public void refreshCryptoPrices() throws Exception {

        try {
            BigDecimal usdToEur = (BigDecimal) getValueRange("investing!F1:F1").getValues().get(0).get(0);
            Map<String, BigDecimal> prices = coinMarketCapService.getPrices(BINANCE_COIN_ID, CRO_ID);

            Map<String, String> cryptoCellsMap = new HashMap<>();
            cryptoCellsMap.put(BINANCE_COIN_ID, "investing!G21:G21");
            cryptoCellsMap.put(CRO_ID, "investing!G22:G22");

            for (Map.Entry<String, String> e : cryptoCellsMap.entrySet()) {
                BigDecimal value = prices.get(e.getKey()).multiply(usdToEur);
                String updateCell = e.getValue();
                googleSheetsClient.update(sheetsService, updateCell, value);
            }

        } catch (Exception e) {
            LOG.error("Error ", e);
            throw new Exception(e.getMessage());
        }
    }

    private BatchUpdateSpreadsheetRequest buildBatchRequest(Spreadsheet spreadsheetResponse, ValueRange investingResponse) {
        Integer sheetID = spreadsheetResponse.getSheets().get(1).getProperties().getSheetId();
        BigDecimal annualReturn = (BigDecimal) investingResponse.getValues().get(0).get(0);
        BigDecimal profit = (BigDecimal) investingResponse.getValues().get(1).get(0);
        BigDecimal totalSavingsAmount = (BigDecimal) investingResponse.getValues().get(2).get(0);
        BigDecimal currentTimeFromSpreadSheet = (BigDecimal) investingResponse.getValues().get(2).get(1);
        Instant timeFromInvesting = Instant.ofEpochSecond(
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

        return batchRequests;
    }

    private void updateTickerAmounts() throws IOException {
        ValueRange valueRange = getValueRange("investing!E29:F32");

        leftOverAmount = (BigDecimal) getValueRange("investing!Q28:Q28").getValues().get(0).get(0);

        Map<String, BigDecimal> values = new HashMap<>();
        for (int i = 0; i < valueRange.getValues().size(); i++) {
            values.put(
                    (String) valueRange.getValues().get(i).get(0),
                    (BigDecimal) valueRange.getValues().get(i).get(1)
            );
        }

        BigDecimal min = values.values().stream().min(Comparator.naturalOrder()).orElse(null);

        int maxTickerAmount = leftOverAmount.divide(min, RoundingMode.DOWN).intValue();

        List<Integer> numbers = IntStream.rangeClosed(0, maxTickerAmount).boxed().collect(Collectors.toList());
        List<List<Integer>> combinations = Generator.combination(numbers)
                .multi(4)
                .stream()
                .collect(Collectors.toList());

        List<List<String>> tickerCombinations = Generator.permutation(values.keySet())
                .simple()
                .stream()
                .collect(Collectors.toList());

        List<Map<String, BigDecimal>> temporaryResult = new ArrayList<>();

        for (List<String> tickerCombination : tickerCombinations) {
            for (String s : tickerCombination) {
                for (List<Integer> combination : combinations) {
                    Map<String, BigDecimal> mapResult = new HashMap<>();
                    for (int i = 0; i < combination.size(); i++) {
                        String key = tickerCombination.get(i);
                        BigDecimal multiply = values.get(key).multiply(BigDecimal.valueOf(combination.get(i)));
                        mapResult.put(key, multiply);
                    }
                    temporaryResult.add(mapResult);
                }
            }
        }

        Map<String, BigDecimal> finalCombination = temporaryResult.stream()
                .filter(this::filter)
                .sorted(Comparator.comparing(Map::values, this::compare))
                .limit(1)
                .findFirst()
                .orElse(null);

        Map<String, Integer> tickerAndAmount = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : finalCombination.entrySet()) {
            int countOfTicker = entry.getValue().divide(values.get(entry.getKey()), BigDecimal.ROUND_UNNECESSARY).intValue();
            tickerAndAmount.put(entry.getKey(), countOfTicker);
        }

        List<Object> collect = getValueRange("investing!E29:E32")
                .getValues()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        HashMap<String, String> objectObjectHashMap = new HashMap<>();
        for (int i = 0; i < collect.size(); i++) {
            String key = (String) collect.get(i);
            objectObjectHashMap.put(key, "investing!R" + (29 + i));
        }

        for (Map.Entry<String, String> e : objectObjectHashMap.entrySet()) {
            String updateCell = e.getValue();
            Integer value = tickerAndAmount.get(e.getKey());
            googleSheetsClient.update(sheetsService, updateCell, value);
        }

    }

    private int compare(Collection<BigDecimal> a, Collection<BigDecimal> b) {
        return b.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(a.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private boolean filter(Map<String, BigDecimal> map) {
        BigDecimal sum = map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return ComparableUtils.is(sum).lessThanOrEqualTo(leftOverAmount) && ComparableUtils.is(sum).greaterThan(BigDecimal.ZERO);
    }

    private ValueRange getValueRange(String range) throws IOException {
        Sheets.Spreadsheets.Values.Get getInvestingRequest =
                sheetsService.spreadsheets().values().get(SPREAD_SHEET_ID, range);
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

    @PostConstruct
    public void createSheetsService() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        Credential httpRequestInitializer = authorizeWithServiceAccount();
        sheetsService = new Sheets.Builder(httpTransport, jsonFactory, httpRequestInitializer)
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
            PrivateKey privateKey = kf.generatePrivate(keySpec);
            return privateKey;
        } catch (Exception e){
            return null;
        }
    }

}
