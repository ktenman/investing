package ee.tenman.investing.integration.google;

import com.google.api.services.sheets.v4.Sheets;
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
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.yieldwatchnet.YieldSummary;
import ee.tenman.investing.integration.yieldwatchnet.YieldWatchService;
import ee.tenman.investing.service.PriceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.paukov.combinatorics3.Generator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ee.tenman.investing.configuration.FetchingConfiguration.BINANCE_COIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.BITCOIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CARDANO_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CRO_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.ETHEREUM_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.ONE_INCH_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.POLKADOT_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.SUSHI_SWAP_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.SYNTHETIX_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static ee.tenman.investing.configuration.FetchingConfiguration.UNISWAP_ID;
import static java.lang.Math.abs;
import static java.math.BigDecimal.ZERO;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;

@Service
@Slf4j
public class GoogleSheetsService {

    public static final String SPREAD_SHEET_ID = "1Buo5586QNMC6v40C0bbD2MTH673dWN12FTgn_oAfIsM";
    private static final String VALUE_RENDER_OPTION = "UNFORMATTED_VALUE";
    private static final String DATE_TIME_RENDER_OPTION = "SERIAL_NUMBER";
    private static final NumberFormat DATE_TIME_FORMAT = new NumberFormat()
            .setType("DATE_TIME")
            .setPattern("dd.mm.yyyy h:mm:ss");
    private static final String EUR = "EUR";
    public static final String WBNB_CURRENCY = "wbnb";
    public static final String BDO_CURRENCY = "bdollar";

    private BigDecimal leftOverAmount;
    @Resource
    PriceService priceService;
    @Resource
    GoogleSheetsClient googleSheetsClient;
    @Resource
    YieldWatchService yieldWatchService;
    @Resource
    BinanceService binanceService;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    @Scheduled(cron = "0 0/5 * * * *")
    public void appendProfits() {
        Spreadsheet spreadsheetResponse = getSpreadSheetResponse();
        if (spreadsheetResponse == null) {
            return;
        }
        ValueRange investingResponse = getValueRange("investing!B1:C3");
        if (investingResponse == null) {
            return;
        }
        Integer sheetID = sheetIndex(spreadsheetResponse, "profits");
        BatchUpdateSpreadsheetRequest profitsBatchRequest = buildProfitsBatchRequest(sheetID, investingResponse);
        googleSheetsClient.update(profitsBatchRequest);
    }

    public Integer sheetIndex(Spreadsheet spreadsheetResponse, String sheetTitle) {
        return spreadsheetResponse.getSheets()
                .stream()
                .map(Sheet::getProperties)
                .filter(sheetProperties -> sheetProperties.getTitle().equals(sheetTitle))
                .map(SheetProperties::getSheetId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("%s sheet not found", sheetTitle)));
    }

    @Scheduled(cron = "0 4/5 * * * *")
    public void appendYieldInformation() throws ExecutionException, InterruptedException {
        Spreadsheet spreadsheetResponse = getSpreadSheetResponse();
        if (spreadsheetResponse == null) {
            return;
        }
        Integer sheetID = sheetIndex(spreadsheetResponse, "yield");
        BatchUpdateSpreadsheetRequest yieldBatchRequest = buildYieldBatchRequest(sheetID);
        googleSheetsClient.update(yieldBatchRequest);
    }

    private BatchUpdateSpreadsheetRequest buildYieldBatchRequest(Integer sheetID) throws ExecutionException, InterruptedException {
        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();
        BigDecimal yieldEarnedPercentage = yieldSummary.getYieldEarnedPercentage();

        List<RowData> rowData = new ArrayList<>();
        List<CellData> cellData = new ArrayList<>();

        CellData yieldEarnedPercentageCell = new CellData();
        yieldEarnedPercentageCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldEarnedPercentage.doubleValue()));
        yieldEarnedPercentageCell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("PERCENT")));
        cellData.add(yieldEarnedPercentageCell);

        CellData wbnbAmountCell = new CellData();
        wbnbAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.getWbnbAmount().doubleValue()));
        cellData.add(wbnbAmountCell);

        CellData wbnbToEurCell = new CellData();
        BigDecimal wbnbToEur = priceService.toEur(WBNB_CURRENCY);
        wbnbToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(wbnbToEur.doubleValue()));
        cellData.add(wbnbToEurCell);

        CellData bdoAmountCell = new CellData();
        bdoAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.getBdoAmount().doubleValue()));
        cellData.add(bdoAmountCell);

        CellData bdoToEurCell = new CellData();
        BigDecimal bdoToEur = priceService.toEur(BDO_CURRENCY);
        bdoToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(bdoToEur.doubleValue()));
        cellData.add(bdoToEurCell);

        CellData totalEurCell = new CellData();
        BigDecimal total = yieldSummary.getBdoAmount().multiply(bdoToEur).add(yieldSummary.getWbnbAmount().multiply(wbnbToEur));
        totalEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(total.doubleValue()));
        cellData.add(totalEurCell);

        CellData earnedYieldCell = new CellData();
        BigDecimal earnedYield = yieldSummary.getYieldEarnedPercentage().multiply(total);
        earnedYieldCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earnedYield.doubleValue()));
        cellData.add(earnedYieldCell);

        Instant now = Instant.now();
        CellData machineDateTimeCell = new CellData();
        machineDateTimeCell.setUserEnteredValue(new ExtendedValue().setNumberValue(now.getEpochSecond() / 86400.0 + 25569));
        machineDateTimeCell.setUserEnteredFormat(new CellFormat().setNumberFormat(DATE_TIME_FORMAT));
        cellData.add(machineDateTimeCell);

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

    @Scheduled(fixedDelay = 60000, initialDelay = 90000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    public void updateSumOfTickers() throws Exception {

        try {
            updateTickerAmounts();
        } catch (Exception e) {
            log.error("Error ", e);
            throw new Exception(e.getMessage());
        }
    }

    public void removeCells() {
        try {
            Spreadsheet spreadsheetResponse = getSpreadSheetResponse();

            SheetProperties properties = spreadsheetResponse.getSheets().get(1).getProperties();

            Integer sheetID = properties.getSheetId();

            Sheets.Spreadsheets.Values.Get getInvestingRequest =
                    googleSheetsClient.get().spreadsheets().values().get(SPREAD_SHEET_ID, properties.getTitle());
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

            BatchUpdateSpreadsheetResponse response = googleSheetsClient.get().spreadsheets()
                    .batchUpdate(SPREAD_SHEET_ID, batchRequests)
                    .execute();

            log.info("{}", response);
        } catch (Exception e) {
            log.error("Error ", e);
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public void refreshCryptoPrices() throws Exception {

        try {
            Map<String, BigDecimal> prices = priceService.getPrices(TICKER_SYMBOL_MAP.keySet());

            Map<String, String> cryptoCellsMap = new HashMap<>();
            cryptoCellsMap.put(BINANCE_COIN_ID, "investing!G21:G21");
            cryptoCellsMap.put(CRO_ID, "investing!G22:G22");
            cryptoCellsMap.put(POLKADOT_ID, "investing!G23:G23");
            cryptoCellsMap.put(UNISWAP_ID, "investing!G24:G24");
            cryptoCellsMap.put(BITCOIN_ID, "investing!G25:G25");
            cryptoCellsMap.put(SUSHI_SWAP_ID, "investing!G26:G26");
            cryptoCellsMap.put(SYNTHETIX_ID, "investing!G27:G27");
            cryptoCellsMap.put(ONE_INCH_ID, "investing!G28:G28");
            cryptoCellsMap.put(CARDANO_ID, "investing!G29:G29");
            cryptoCellsMap.put(ETHEREUM_ID, "investing!G30:G30");

            for (Map.Entry<String, String> e : cryptoCellsMap.entrySet()) {
                String updateCell = e.getValue();
                googleSheetsClient.update(updateCell, prices.get(e.getKey()));
            }

            YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

            googleSheetsClient.update("investing!M1:M1", yieldSummary.getYieldEarnedPercentage());
            BigDecimal wbnbToEurPrice = priceService.toEur(WBNB_CURRENCY);
            if (ComparableUtils.is(wbnbToEurPrice).greaterThan(ZERO)) {
                googleSheetsClient.update("investing!G31:G31", wbnbToEurPrice);
            } else {
                googleSheetsClient.update("investing!G31:G31", binanceService.getPriceToEur("BNB"));
            }
            googleSheetsClient.update("investing!F31:F31", yieldSummary.getWbnbAmount());
            BigDecimal bdoToEurPrice = priceService.toEur(BDO_CURRENCY);
            if (ComparableUtils.is(bdoToEurPrice).greaterThan(ZERO)) {
                googleSheetsClient.update("investing!G32:G32", bdoToEurPrice);
            }
            googleSheetsClient.update("investing!F32:F32", yieldSummary.getBdoAmount());
        } catch (Exception e) {
            log.error("Error ", e);
            throw new Exception(e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 36_000, initialDelay = 6_000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public void refreshBalances() throws Exception {
        int startingIndexNumber = 21;
        String startingIndexCombined = "E" + startingIndexNumber;
        ValueRange valueRange = getValueRange(String.format("investing!%s:E30", startingIndexCombined));
        String[] values = Objects.requireNonNull(valueRange).getValues().stream().flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new);

        List<String> symbols = new ArrayList<>(TICKER_SYMBOL_MAP.values());
        symbols.add(EUR);

        Map<String, BigDecimal> availableBalances = binanceService.fetchAvailableBalances(symbols);

        try {
            for (int i = 0; i < values.length; i++) {
                for (Map.Entry<String, BigDecimal> entry : availableBalances.entrySet()) {
                    if (entry.getKey().equals(values[i])) {
                        String coordinate = "D" + (startingIndexNumber + i);
                        String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
                        googleSheetsClient.update(coordinates, entry.getValue());
                    }
                }
            }
            googleSheetsClient.update("investing!L34:L34", availableBalances.get(EUR));
        } catch (Exception e) {
            log.error("Error ", e);
            throw new Exception(e.getMessage());
        }
    }

    private BatchUpdateSpreadsheetRequest buildProfitsBatchRequest(Integer sheetID, ValueRange investingResponse) {
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
        machineDateTimeCell.setUserEnteredValue(new ExtendedValue().setNumberValue(now.getEpochSecond() / 86400.0 + 25569));
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
        ValueRange valueRange = getValueRange("investing!E37:F40");

        leftOverAmount = (BigDecimal) getValueRange("investing!Q36:Q36").getValues().get(0).get(0);

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

        List<Object> collect = getValueRange("investing!E37:E40")
                .getValues()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        HashMap<String, String> objectObjectHashMap = new HashMap<>();
        for (int i = 0; i < collect.size(); i++) {
            String key = (String) collect.get(i);
            objectObjectHashMap.put(key, "investing!R" + (37 + i));
        }

        for (Map.Entry<String, String> e : objectObjectHashMap.entrySet()) {
            String updateCell = e.getValue();
            Integer value = tickerAndAmount.get(e.getKey());
            googleSheetsClient.update(updateCell, value);
        }

    }

    private int compare(Collection<BigDecimal> a, Collection<BigDecimal> b) {
        return b.stream().reduce(ZERO, BigDecimal::add)
                .compareTo(a.stream().reduce(ZERO, BigDecimal::add));
    }

    private boolean filter(Map<String, BigDecimal> map) {
        BigDecimal sum = map.values().stream().reduce(ZERO, BigDecimal::add);
        return ComparableUtils.is(sum).lessThanOrEqualTo(leftOverAmount) && ComparableUtils.is(sum).greaterThan(ZERO);
    }

    private ValueRange getValueRange(String range) {
        try {
            Sheets.Spreadsheets.Values.Get getInvestingRequest =
                    googleSheetsClient.get().spreadsheets().values().get(SPREAD_SHEET_ID, range);
            getInvestingRequest.setValueRenderOption(VALUE_RENDER_OPTION);
            getInvestingRequest.setDateTimeRenderOption(DATE_TIME_RENDER_OPTION);
            return getInvestingRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Spreadsheet getSpreadSheetResponse() {
        try {
            boolean includeGridData = false;
            Sheets.Spreadsheets.Get spreadsheetRequest = googleSheetsClient.get().spreadsheets().get(SPREAD_SHEET_ID);
            spreadsheetRequest.setRanges(new ArrayList<>());
            spreadsheetRequest.setIncludeGridData(includeGridData);
            return spreadsheetRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
