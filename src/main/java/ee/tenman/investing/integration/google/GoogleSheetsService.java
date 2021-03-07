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
import ee.tenman.investing.integration.bscscan.BscScanService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.integration.yieldwatchnet.YieldSummary;
import ee.tenman.investing.integration.yieldwatchnet.YieldWatchService;
import ee.tenman.investing.service.PriceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ee.tenman.investing.configuration.FetchingConfiguration.BINANCE_COIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.BITCOIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CARDANO_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CRO_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.ETHEREUM_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.POLKADOT_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.SUSHI_SWAP_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static ee.tenman.investing.configuration.FetchingConfiguration.UNISWAP_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.USDT_ID;
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

    private BigDecimal leftOverAmount;
    @Resource
    private PriceService priceService;
    @Resource
    private GoogleSheetsClient googleSheetsClient;
    @Resource
    private YieldWatchService yieldWatchService;
    @Resource
    private BinanceService binanceService;
    @Resource
    private BscScanService bscScanService;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    @Scheduled(cron = "0 0/5 * * * *")
    public void appendProfits() {
        Spreadsheet spreadsheetResponse = getSpreadSheetResponse(SPREAD_SHEET_ID);
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

    @Scheduled(cron = "0 15/30 * * * *")
    public void appendYieldInformation() {

        Spreadsheet spreadsheetResponse = getSpreadSheetResponse(SPREAD_SHEET_ID);
        if (spreadsheetResponse == null) {
            return;
        }
        Integer sheetID = sheetIndex(spreadsheetResponse, "yield");
        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();
        BatchUpdateSpreadsheetRequest yieldBatchRequest = buildYieldBatchRequest(sheetID, yieldSummary);
        googleSheetsClient.update(yieldBatchRequest);
    }

    boolean continueWorking() {
        return RandomUtils.nextInt(0, 100) >= 99;
    }

    private BatchUpdateSpreadsheetRequest buildYieldBatchRequest(Integer sheetID, YieldSummary yieldSummary) {
        BigDecimal yieldEarnedPercentage = yieldSummary.getYieldEarnedPercentage();

        List<RowData> rowData = new ArrayList<>();
        List<CellData> cellData = new ArrayList<>();

        CellData yieldEarnedPercentageCell = new CellData();
        yieldEarnedPercentageCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldEarnedPercentage.doubleValue()));
        yieldEarnedPercentageCell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("PERCENT")));
        cellData.add(yieldEarnedPercentageCell);

        CellData wbnbAmountCell = new CellData();
        wbnbAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.amountOf(Symbol.WBNB).doubleValue()));
        cellData.add(wbnbAmountCell);

        CellData wbnbToEurCell = new CellData();
        BigDecimal wbnbToEur = priceService.toEur(Symbol.WBNB);
        wbnbToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(wbnbToEur.doubleValue()));
        cellData.add(wbnbToEurCell);

        CellData busdAmountCell = new CellData();
        busdAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.amountOf(Symbol.BUSD).doubleValue()));
        cellData.add(busdAmountCell);

        CellData busdToEurCell = new CellData();
        BigDecimal busdToEur = priceService.toEur(Symbol.BUSD);
        busdToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(busdToEur.doubleValue()));
        cellData.add(busdToEurCell);

        CellData bdoAmountCell = new CellData();
        bdoAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.amountOf(Symbol.BDO).doubleValue()));

        CellData bdoToEurCell = new CellData();
        BigDecimal bdoToEur = priceService.toEur(Symbol.BDO);
        bdoToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(bdoToEur.doubleValue()));

        CellData sbdoAmountCell = new CellData();
        sbdoAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.amountOf(Symbol.SBDO).doubleValue()));
        CellData sbdoToEurCell = new CellData();
        BigDecimal sbdoToEur = priceService.toEur(Symbol.SBDO);
        sbdoToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(sbdoToEur.doubleValue()));

        CellData watchAmountCell = new CellData();
        watchAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.amountOf(Symbol.WATCH).doubleValue()));
        CellData watchToEurCell = new CellData();
        BigDecimal watchToEur = priceService.toEur(Symbol.WATCH);
        watchToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(watchToEur.doubleValue()));

        CellData cakeAmountCell = new CellData();
        cakeAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldSummary.amountOf(Symbol.CAKE).doubleValue()));
        CellData cakeToEurCell = new CellData();
        BigDecimal cakeToEur = priceService.toEur(Symbol.CAKE);
        cakeToEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(cakeToEur.doubleValue()));

        CellData totalEurCell = new CellData();
        BigDecimal total = yieldSummary.amountOf(Symbol.BUSD).multiply(busdToEur)
                .add(yieldSummary.amountOf(Symbol.WBNB).multiply(wbnbToEur))
                .add(yieldSummary.amountOf(Symbol.BDO).multiply(bdoToEur))
                .add(yieldSummary.amountOf(Symbol.SBDO).multiply(sbdoToEur))
                .add(yieldSummary.amountOf(Symbol.WATCH).multiply(watchToEur)
                        .add(yieldSummary.amountOf(Symbol.CAKE).multiply(cakeToEur)));
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

        BigDecimal investedBnbAmount = (BigDecimal) getValueRange("investing!H21:H21").getValues().get(0).get(0);
        BigDecimal totalEurInvested = wbnbToEur.multiply(investedBnbAmount);

        CellData investedEurDifferenceCell = new CellData();
        BigDecimal investedEurDifference = total.subtract(totalEurInvested);
        investedEurDifferenceCell.setUserEnteredValue(new ExtendedValue().setNumberValue(investedEurDifference.doubleValue()));
        cellData.add(investedEurDifferenceCell);

        CellData earnedBnbAmountCell = new CellData();
        BigDecimal earnedBnb = investedEurDifference.divide(wbnbToEur, RoundingMode.HALF_UP);
        earnedBnbAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earnedBnb.doubleValue()));
        cellData.add(earnedBnbAmountCell);

        cellData.add(bdoAmountCell);
        cellData.add(bdoToEurCell);
        cellData.add(sbdoAmountCell);
        cellData.add(sbdoToEurCell);

        CellData earningsPerDayCell = new CellData();
        BigDecimal earningsPerDay = (BigDecimal) getValueRange("yield!Y1:Y1").getValues().get(0).get(0);
        earningsPerDayCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earningsPerDay.doubleValue()));
        cellData.add(earningsPerDayCell);

        for (BigDecimal value : yieldSummary.getPools().values()) {
            CellData newCellData = new CellData();
            newCellData.setUserEnteredValue(new ExtendedValue().setNumberValue(value.doubleValue()));
            cellData.add(newCellData);
        }

        cellData.add(watchAmountCell);
        cellData.add(watchToEurCell);
        cellData.add(cakeAmountCell);
        cellData.add(cakeToEurCell);

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
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void updateSumOfTickers() throws IOException {
        updateTickerAmounts();
    }

    public void removeCells() throws IOException {
        Spreadsheet spreadsheetResponse = getSpreadSheetResponse(SPREAD_SHEET_ID);

        SheetProperties properties = spreadsheetResponse.getSheets().get(1).getProperties();
        Integer sheetID = sheetIndex(spreadsheetResponse, "yield");

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
        dimensionRange.setStartIndex(10000);
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
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void refreshCryptoPrices() throws IOException {
        Map<String, BigDecimal> prices = priceService.getPrices(TICKER_SYMBOL_MAP.keySet());

        Map<String, String> cryptoCellsMap = new HashMap<>();
        cryptoCellsMap.put(BINANCE_COIN_ID, "investing!G21:G21");
        cryptoCellsMap.put(CRO_ID, "investing!G22:G22");
        cryptoCellsMap.put(POLKADOT_ID, "investing!G23:G23");
        cryptoCellsMap.put(UNISWAP_ID, "investing!G24:G24");
        cryptoCellsMap.put(BITCOIN_ID, "investing!G25:G25");
        cryptoCellsMap.put(SUSHI_SWAP_ID, "investing!G26:G26");
        cryptoCellsMap.put(USDT_ID, "investing!G28:G28");
        cryptoCellsMap.put(CARDANO_ID, "investing!G29:G29");
        cryptoCellsMap.put(ETHEREUM_ID, "investing!G27:G27");

        for (Map.Entry<String, String> e : cryptoCellsMap.entrySet()) {
            String updateCell = e.getValue();
            googleSheetsClient.update(updateCell, prices.get(e.getKey()));
        }

        BigDecimal sbdoToEurPrice = priceService.toEur(Symbol.SBDO);
        if (ComparableUtils.is(sbdoToEurPrice).greaterThan(ZERO)) {
            googleSheetsClient.update("investing!G30:G30", sbdoToEurPrice);
        }

        BigDecimal wbnbToEurPrice = priceService.toEur(Symbol.WBNB);
        if (ComparableUtils.is(wbnbToEurPrice).greaterThan(ZERO)) {
            googleSheetsClient.update("investing!G31:G31", wbnbToEurPrice);
        }

        BigDecimal bdoToEurPrice = priceService.toEur(Symbol.BDO);
        if (ComparableUtils.is(bdoToEurPrice).greaterThan(ZERO)) {
            googleSheetsClient.update("investing!G32:G32", bdoToEurPrice);
        }

        BigDecimal busdToEurPrice = priceService.toEur(Symbol.BUSD);
        if (ComparableUtils.is(busdToEurPrice).greaterThan(ZERO)) {
            googleSheetsClient.update("investing!G33:G33", busdToEurPrice);
        }
        BigDecimal cakeToEurPrice = priceService.toEur(Symbol.CAKE);
        if (ComparableUtils.is(cakeToEurPrice).greaterThan(ZERO)) {
            googleSheetsClient.update("investing!G34:G34", cakeToEurPrice);
        }

        BigDecimal watchToEurPrice = priceService.toEur(Symbol.WATCH);
        if (ComparableUtils.is(watchToEurPrice).greaterThan(ZERO)) {
            googleSheetsClient.update("investing!G35:G35", watchToEurPrice);
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void refreshBalances() throws IOException {
        int startingIndexNumber = 21;
        String startingIndexCombined = "E" + startingIndexNumber;
        ValueRange valueRange = getValueRange(String.format("investing!%s:E29", startingIndexCombined));
        String[] values = Objects.requireNonNull(valueRange).getValues().stream().flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new);

        List<String> symbols = new ArrayList<>(TICKER_SYMBOL_MAP.values());
        symbols.add(EUR);

        Map<String, BigDecimal> availableBalances = binanceService.fetchAvailableBalances(symbols);

        for (int i = 0; i < values.length; i++) {
            for (Map.Entry<String, BigDecimal> entry : availableBalances.entrySet()) {
                if (entry.getKey().equals(values[i])) {
                    String coordinate = "D" + (startingIndexNumber + i);
                    String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
                    googleSheetsClient.update(coordinates, entry.getValue());
                }
            }
        }
        googleSheetsClient.update("investing!L39:L39", availableBalances.get(EUR));
        googleSheetsClient.update("investing!F21:F21", bscScanService.getBnbBalance());
        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();
        googleSheetsClient.update("investing!M1:M1", yieldSummary.getYieldEarnedPercentage());

        startingIndexNumber = 30;
        startingIndexCombined = "E" + startingIndexNumber;
        valueRange = getValueRange(String.format("investing!%s:E35", startingIndexCombined));
        values = Objects.requireNonNull(valueRange).getValues().stream().flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new);

        for (int i = 0; i < values.length; i++) {
            String coordinate = "F" + (startingIndexNumber + i);
            String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
            String value = values[i];
            BigDecimal amount = yieldSummary.amountOf(Symbol.valueOf(value));
            googleSheetsClient.update(coordinates, amount);
        }
    }

    private BatchUpdateSpreadsheetRequest buildProfitsBatchRequest(Integer sheetID, ValueRange investingResponse) {
        BigDecimal annualReturn = (BigDecimal) investingResponse.getValues().get(0).get(0);
        BigDecimal profit = (BigDecimal) investingResponse.getValues().get(1).get(0);
        BigDecimal totalSavingsAmount = ((BigDecimal) investingResponse.getValues().get(2).get(0)).abs();
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
        ValueRange valueRange = getValueRange("investing!E45:F48");

        leftOverAmount = (BigDecimal) getValueRange("investing!Q44:Q44").getValues().get(0).get(0);

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

    public Spreadsheet getSpreadSheetResponse(String spreadSheetId) {
        try {
            boolean includeGridData = false;
            Sheets.Spreadsheets.Get spreadsheetRequest = googleSheetsClient.get().spreadsheets().get(spreadSheetId);
            spreadsheetRequest.setRanges(new ArrayList<>());
            spreadsheetRequest.setIncludeGridData(includeGridData);
            return spreadsheetRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
