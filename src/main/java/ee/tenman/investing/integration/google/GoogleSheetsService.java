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
import com.google.common.collect.ImmutableList;
import ee.tenman.investing.domain.StockSymbol;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.bscscan.BalanceService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.integration.yieldwatchnet.YieldSummary;
import ee.tenman.investing.integration.yieldwatchnet.YieldWatchService;
import ee.tenman.investing.service.PriceService;
import ee.tenman.investing.service.SecretsService;
import ee.tenman.investing.service.StockPriceService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.paukov.combinatorics3.Generator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static ee.tenman.investing.integration.google.GoogleSheetsClient.DATE_TIME_RENDER_OPTION;
import static ee.tenman.investing.integration.google.GoogleSheetsClient.VALUE_RENDER_OPTION;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.AUTO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BDO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BNB;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BUSD;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.CAKE;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.MDX;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.SBDO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.WATCH;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.WBNB;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.valueOf;
import static java.lang.Math.abs;
import static java.math.BigDecimal.ZERO;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.naturalOrder;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class GoogleSheetsService {

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
    private BalanceService balanceService;
    @Resource
    private StockPriceService stockPriceService;
    @Resource
    private SecretsService secretsService;
    @Value("${sheet-id:}")
    private String sheetId;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
//    @Scheduled(cron = "0 0/10 * * * *")
    @Scheduled(cron = "0 0 * * * *")
    public void appendProfits() {
        Spreadsheet spreadsheetResponse = googleSheetsClient.getSpreadSheetResponse();
        if (spreadsheetResponse == null) {
            return;
        }
        ValueRange investingResponse = googleSheetsClient.getValueRange("investing!B1:C3");
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

//    @Scheduled(cron = "0 0 * * * *")
    public void appendYieldInformation() {

        Spreadsheet spreadsheetResponse = googleSheetsClient.getSpreadSheetResponse();
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
        Map<Symbol, BigDecimal> prices = priceService.getPricesOfBalances(yieldSummary.getPoolBalances());

        BigDecimal yieldEarnedPercentage = yieldSummary.getYieldEarnedPercentage();

        List<RowData> rowData = new ArrayList<>();
        List<CellData> cellData = new ArrayList<>();

        Instant now = Instant.now();
        CellData machineDateTimeCell = new CellData();
        machineDateTimeCell.setUserEnteredValue(new ExtendedValue().setNumberValue(now.getEpochSecond() / 86400.0 + 25569));
        machineDateTimeCell.setUserEnteredFormat(new CellFormat().setNumberFormat(DATE_TIME_FORMAT));
        cellData.add(machineDateTimeCell);

        CellData totalEurCell = new CellData();
        BigDecimal total = yieldSummary.getTotal(prices);
        totalEurCell.setUserEnteredValue(new ExtendedValue().setNumberValue(total.doubleValue()));
        cellData.add(totalEurCell);

        CellData earnedYieldCell = new CellData();
        BigDecimal earnedYield = yieldSummary.getYieldEarnedPercentage().multiply(total);
        earnedYieldCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earnedYield.doubleValue()));
        cellData.add(earnedYieldCell);

        CellData yieldPerDayCell = new CellData();
        BigDecimal yieldPerDay = extractValueRangeFrom("yield!A1:A1", "Couldn't fetch yield per day value");
        yieldPerDayCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldPerDay.doubleValue()));
        cellData.add(yieldPerDayCell);

        CellData earningsPerDayCell = new CellData();
        BigDecimal earningsPerDay = extractValueRangeFrom("yield!C1:C1", "Couldn't fetch earnings per day value");
        earningsPerDayCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earningsPerDay.doubleValue()));
        cellData.add(earningsPerDayCell);

        CellData yieldEarnedPercentageCell = new CellData();
        yieldEarnedPercentageCell.setUserEnteredValue(new ExtendedValue().setNumberValue(yieldEarnedPercentage.doubleValue()));
        yieldEarnedPercentageCell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("PERCENT")));
        cellData.add(yieldEarnedPercentageCell);

        CellData earningsPerDayRoiCell = new CellData();
        BigDecimal earningsPerRoiDay = extractBigDecimalFromValueRange("yield!B1:B1");
        earningsPerDayRoiCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earningsPerRoiDay.doubleValue()));
        earningsPerDayRoiCell.setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("PERCENT")));
        cellData.add(earningsPerDayRoiCell);

        BigDecimal investedBnbAmount = BigDecimal.valueOf(13.6048272167);
        BigDecimal totalEurInvested = prices.get(WBNB).multiply(investedBnbAmount);

        CellData investedEurDifferenceCell = new CellData();
        BigDecimal investedEurDifference = total.subtract(totalEurInvested);
        investedEurDifferenceCell.setUserEnteredValue(new ExtendedValue().setNumberValue(investedEurDifference.doubleValue()));

        CellData earnedBnbAmountCell = new CellData();
        BigDecimal earnedBnb = investedEurDifference.divide(prices.get(WBNB), RoundingMode.HALF_UP);
        earnedBnbAmountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(earnedBnb.doubleValue()));

        cellData.add(earnedBnbAmountCell);
        cellData.add(investedEurDifferenceCell);

        ValueRange values = googleSheetsClient.getValueRange("yield!J2:Y2");

        List<String> headers = Stream.of(Objects.requireNonNull(values).getValues()
                .stream()
                .flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new))
                .collect(toList());

        headers.forEach(header -> {
            if (header.contains("-")) {
                log.info("header: {}", header);
                CellData newCellData = new CellData();
                BigDecimal value = yieldSummary.getPools().getOrDefault(header, ZERO);
                newCellData.setUserEnteredValue(new ExtendedValue().setNumberValue(value.doubleValue()));
                cellData.add(newCellData);
                log.info("{} pool: {}", header, value);
                return;
            }
            if (!header.contains("€")) {
                CellData amountCell = new CellData();
                BigDecimal amount = yieldSummary.amountInPool(valueOf(header));
                amountCell.setUserEnteredValue(new ExtendedValue().setNumberValue(amount.doubleValue()));
                cellData.add(amountCell);
                log.info("{} amount: {}", header, amount);
                return;
            }
            Symbol symbol = Symbol.valueOf(header.split(" ")[0]);
            CellData priceCell = new CellData();
            BigDecimal price = prices.getOrDefault(symbol, ZERO);
            priceCell.setUserEnteredValue(new ExtendedValue().setNumberValue(price.doubleValue()));
            cellData.add(priceCell);
            log.info("{} price: {}", header, price);
        });

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

    private BigDecimal extractValueRangeFrom(String valueRange, String errorMessage) {
        return Optional.ofNullable(googleSheetsClient.getValueRange(valueRange))
                .map(ValueRange::getValues)
                .map(o -> o.get(0))
                .map(o -> o.get(0))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .map(text -> text.replaceAll("[^\\d.]", ""))
                .map(BigDecimal::new)
                .orElseThrow(() -> new IllegalStateException(errorMessage));
    }

    private BigDecimal extractBigDecimalFromValueRange(String valueRange) {
        return Optional.ofNullable(googleSheetsClient.getValueRange(valueRange))
                .map(ValueRange::getValues)
                .map(o -> o.get(0))
                .map(o -> o.get(0))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .map(text -> text.replaceAll("[^\\d.]", ""))
                .map(BigDecimal::new)
                .orElse(ZERO);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 20, backoff = @Backoff(delay = 2000))
    public String extractDateFromValueRange(String valueRange) {
        return googleSheetsClient.getOptionalValueRange(valueRange)
                .map(ValueRange::getValues)
                .map(o -> o.get(0))
                .map(o -> o.get(0))
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .map(text -> text.replaceAll("[^\\d.]", ""))
                .orElse("");
    }


    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void updateSumOfTickers() throws IOException {
        updateTickerAmounts();
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @SneakyThrows
    public void removeCells(String sheet, int startIndex, int endIndex) {
        Spreadsheet spreadsheetResponse = googleSheetsClient.getSpreadSheetResponse();

        SheetProperties properties = spreadsheetResponse.getSheets().get(1).getProperties();
        Integer sheetID = sheetIndex(spreadsheetResponse, sheet);

        Sheets.Spreadsheets.Values.Get getInvestingRequest =
                googleSheetsClient.get().spreadsheets().values().get(sheetId, properties.getTitle());
        getInvestingRequest.setValueRenderOption(VALUE_RENDER_OPTION);
        getInvestingRequest.setDateTimeRenderOption(DATE_TIME_RENDER_OPTION);

        ValueRange valueRange = getInvestingRequest.execute();

        String maximum = valueRange.getRange().split(":")[1].replaceAll("[^\\d.]", "");
        int setEndIndex = Integer.parseInt(maximum);

        DeleteDimensionRequest deleteDimensionRequest = new DeleteDimensionRequest();
        DimensionRange dimensionRange = new DimensionRange();
        dimensionRange.setSheetId(sheetID);
        dimensionRange.setDimension("ROWS");
        dimensionRange.setStartIndex(startIndex);
        dimensionRange.setEndIndex(endIndex);

        deleteDimensionRequest.setRange(dimensionRange);

        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setDeleteDimension(deleteDimensionRequest));
        BatchUpdateSpreadsheetRequest batchRequests = new BatchUpdateSpreadsheetRequest();
        batchRequests.setRequests(requests);

        BatchUpdateSpreadsheetResponse response = googleSheetsClient.get().spreadsheets()
                .batchUpdate(sheetId, batchRequests)
                .execute();

        log.info("{}", response);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @SneakyThrows
    public void removeCells2(String sheet) {
        Spreadsheet spreadsheetResponse = googleSheetsClient.getSpreadSheetResponse();

        SheetProperties properties = spreadsheetResponse.getSheets().get(1).getProperties();
        Integer sheetID = sheetIndex(spreadsheetResponse, sheet);

        Sheets.Spreadsheets.Values.Get getInvestingRequest =
                googleSheetsClient.get().spreadsheets().values().get(sheetId, properties.getTitle());
        getInvestingRequest.setValueRenderOption(VALUE_RENDER_OPTION);
        getInvestingRequest.setDateTimeRenderOption(DATE_TIME_RENDER_OPTION);

        ValueRange valueRange = getInvestingRequest.execute();

        String maximum = valueRange.getRange().split(":")[1].replaceAll("[^\\d.]", "");
        int setEndIndex = Integer.parseInt(maximum);

        DeleteDimensionRequest deleteDimensionRequest = new DeleteDimensionRequest();
        DimensionRange dimensionRange = new DimensionRange();
        dimensionRange.setSheetId(sheetID);
        dimensionRange.setDimension("ROWS");
        dimensionRange.setStartIndex(20000);
        dimensionRange.setEndIndex(setEndIndex);

        deleteDimensionRequest.setRange(dimensionRange);

        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setDeleteDimension(deleteDimensionRequest));
        BatchUpdateSpreadsheetRequest batchRequests = new BatchUpdateSpreadsheetRequest();
        batchRequests.setRequests(requests);

        BatchUpdateSpreadsheetResponse response = googleSheetsClient.get().spreadsheets()
                .batchUpdate(sheetId, batchRequests)
                .execute();

        log.info("{}", response);
    }


    @Scheduled(fixedDelay = 300_000, initialDelay = 100_000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void refreshCryptoPrices() {

        int index = 21;
        ValueRange valueRange = googleSheetsClient.getValueRange(String.format("investing!E%s:E37", index));
        List<Symbol> values = Stream.of(Objects.requireNonNull(valueRange).getValues()
                .stream()
                .flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new))
                .map(s -> s.replace(":", "_"))
                .map(Symbol::valueOf)
                .collect(toList());

        IntStream.range(0, values.size())
                .parallel()
                .forEach(updateCryptoPriceInGoogleSheets(index, values));
    }

    private IntConsumer updateCryptoPriceInGoogleSheets(int index, List<Symbol> values) {
        return (i) -> {
            String coordinate = "G" + (index + i);
            String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
            try {
                googleSheetsClient.update(coordinates, priceService.toEur(values.get(i)).doubleValue());
            } catch (IOException e) {
                log.error("Failed to update {} with {}", coordinates, values.get(i));
            }
        };
    }

    @Scheduled(cron = "0 5/10 * * * *")
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void refreshStockPrices() throws IOException {
        ValueRange valueRange = googleSheetsClient.getValueRange("investing!E4:E20");
        List<StockSymbol> stockSymbols = Stream.of(Objects.requireNonNull(valueRange).getValues().stream().flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new))
                .map(s -> s.replace(":", "_"))
                .map(StockSymbol::valueOf)
                .collect(toList());

        Map<StockSymbol, BigDecimal> prices = stockPriceService.priceInEur(stockSymbols);

        for (int i = 0; i < stockSymbols.size(); i++) {
            String coordinate = "G" + (4 + i);
            String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
            googleSheetsClient.update(coordinates, prices.get(stockSymbols.get(i)));
        }
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 200_000)
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public void refreshBalances() throws IOException {
        List<String> symbols = new ArrayList<>(TICKER_SYMBOL_MAP.values());
        symbols.add(EUR);
        symbols.add(BUSD.name());
        List<Symbol> symbolList = ImmutableList.of(
                SBDO,
                WBNB,
                BDO,
                BUSD,
                CAKE,
                WATCH,
                AUTO,
                MDX
        );

        CompletableFuture<Map<String, BigDecimal>> availableBalancesFuture = supplyAsync(
                () -> binanceService.fetchAvailableBalances(symbols));
//        CompletableFuture<YieldSummary> yieldSummaryFuture = supplyAsync(
//                () -> yieldWatchService.getYieldSummary());
        CompletableFuture<Map<Symbol, BigDecimal>> balancesFuture = supplyAsync(
                () -> balanceService.fetchSymbolBalances(secretsService.getWalletAddress(), symbolList));
        CompletableFuture<ValueRange> symbolsOfBinanceComWalletFuture = supplyAsync(
                () -> googleSheetsClient.getValueRange("investing!E21:E29"));
        CompletableFuture<ValueRange> symbolsOfBscWalletFuture = supplyAsync(
                () -> googleSheetsClient.getValueRange("investing!E30:E37"));

        Map<String, BigDecimal> availableBalances = availableBalancesFuture.join();
//        YieldSummary yieldSummary = yieldSummaryFuture.join();
        Map<Symbol, BigDecimal> balances = balancesFuture.join();
        ValueRange symbolsOfBinanceComWallet = symbolsOfBinanceComWalletFuture.join();
        ValueRange symbolsOfBscWallet = symbolsOfBscWalletFuture.join();

        String[] values = Objects.requireNonNull(symbolsOfBinanceComWallet)
                .getValues()
                .stream()
                .flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new);

        for (int i = 0; i < values.length; i++) {
            for (Map.Entry<String, BigDecimal> entry : availableBalances.entrySet()) {
                if (entry.getKey().equals(values[i])) {
                    String coordinate = "D" + (21 + i);
                    String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
                    googleSheetsClient.update(coordinates, entry.getValue());
                }
            }
        }
        googleSheetsClient.update("investing!L39:L39", availableBalances.get(EUR));
        googleSheetsClient.update("investing!F21:F21", balances.get(BNB));
//        googleSheetsClient.update("investing!M1:M1", yieldSummary.getYieldEarnedPercentage());

        values = Objects.requireNonNull(symbolsOfBscWallet).getValues()
                .stream()
                .flatMap(Collection::stream)
                .map(v -> (String) v)
                .toArray(String[]::new);

        int startingIndexNumber = 30;
        for (int i = 0; i < values.length; i++) {
            String coordinate = "F" + (startingIndexNumber + i);
            String coordinates = String.format("investing!%s:%s", coordinate, coordinate);
            String value = values[i];
            Symbol symbol = Symbol.valueOf(value);

//            BigDecimal poolAmount = yieldSummary.amountInPool(symbol);
//            googleSheetsClient.update(coordinates, poolAmount);

            coordinate = "D" + (startingIndexNumber + i);
            coordinates = String.format("investing!%s:%s", coordinate, coordinate);

            BigDecimal walletAmount = balances.getOrDefault(symbol, ZERO);
            googleSheetsClient.update(coordinates, walletAmount);
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

        CellData total = new CellData();
        total.setUserEnteredValue(new ExtendedValue().setNumberValue(totalSavingsAmount.doubleValue()));
        cellData.add(total);

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

        CellData earningsPerDay = new CellData();
        double earnings = totalSavingsAmount.multiply(annualReturn)
                .divide(BigDecimal.valueOf(365.25), RoundingMode.HALF_UP).doubleValue();
        earningsPerDay.setUserEnteredValue(new ExtendedValue().setNumberValue(earnings));
        cellData.add(earningsPerDay);

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
        leftOverAmount = extractBigDecimalFromValueRange("investing!Q44:Q44");

        if (leftOverAmount.equals(ZERO)) {
            log.info("Skipping of updating ticker amounts");
            return;
        }

        ValueRange valueRange = googleSheetsClient.getValueRange("investing!E45:F48");
        Map<String, BigDecimal> values = new HashMap<>();
        for (int i = 0; i < valueRange.getValues().size(); i++) {
            values.put(
                    (String) valueRange.getValues().get(i).get(0),
                    (BigDecimal) valueRange.getValues().get(i).get(1)
            );
        }

        BigDecimal min = values.values().stream().min(naturalOrder()).orElse(null);

        int maxTickerAmount = leftOverAmount.divide(min, RoundingMode.DOWN).intValue();

        List<Integer> numbers = IntStream.rangeClosed(0, maxTickerAmount).boxed().collect(toList());
        List<List<Integer>> combinations = Generator.combination(numbers)
                .multi(4)
                .stream()
                .collect(toList());

        List<List<String>> tickerCombinations = Generator.permutation(values.keySet())
                .simple()
                .stream()
                .collect(toList());

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
        for (Map.Entry<String, BigDecimal> entry : Objects.requireNonNull(finalCombination).entrySet()) {
            int countOfTicker = entry.getValue().divide(values.get(entry.getKey()), BigDecimal.ROUND_UNNECESSARY).intValue();
            tickerAndAmount.put(entry.getKey(), countOfTicker);
        }

        List<Object> collect = googleSheetsClient.getValueRange("investing!E45:E48")
                .getValues()
                .stream()
                .flatMap(List::stream)
                .collect(toList());

        HashMap<String, String> objectObjectHashMap = new HashMap<>();
        for (int i = 0; i < collect.size(); i++) {
            String key = (String) collect.get(i);
            objectObjectHashMap.put(key, "investing!R" + (45 + i));
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

    @SneakyThrows
    @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(delay = 2000))
    public void clean(int startingAt) {
        int startIndex = -1;
        int endIndex = -1;
        for (int i = startingAt; i < 200000; i++) {
            TimeUnit.MILLISECONDS.sleep(600);
            String sheet = "profits";
            String index = String.format("%s!E%s:E%s", sheet, i, i);
            String str = extractDateFromValueRange(index);
            BigDecimal bigDecimal = new BigDecimal(str)
                    .subtract(BigDecimal.valueOf(25569))
                    .multiply(BigDecimal.valueOf(86400));
            LocalDateTime date = date(bigDecimal.longValue());
            boolean isCorrectMinute = isCorrectMinute(date);
            if (isCorrectMinute) {
                log.info("Correct! Index: {}, Date: {}", i, date);
                if (startIndex != -1 && endIndex != -1) {
                    removeCells(sheet, startIndex, endIndex);
                    log.info("Success! Index: {}", startIndex);
                    i = startIndex;
                }
                startIndex = -1;
                endIndex = -1;
                continue;
            }
            log.info("Wrong!");
            if (startIndex == -1) {
                startIndex = i - 1;
            }
            endIndex = i;
        }
    }

    private LocalDateTime date(long epochSeconds) {
        return LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC);
    }

    private boolean isCorrectMinute(LocalDateTime localDateTime) {
        return localDateTime.getMinute() == 59 || localDateTime.getMinute() <= 1;
    }

}
