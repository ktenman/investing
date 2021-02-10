package ee.tenman.investing.service.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.binance.api.client.domain.market.CandlestickInterval.HOURLY;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;

@SpringBootTest
class StatsServiceIntegrationTest {

    @Resource
    private StatsService statsService;

    @Resource
    private ObjectMapper objectMapper;

    private static Stream<Arguments> dateProvider() {
        return Stream.of(
                Arguments.of(false, Arrays.asList("BTC", "BNB", "ADA", "ETH")),
                Arguments.of(true, Arrays.asList("BTC", "BNB", "ADA", "ETH")),
                Arguments.of(false, Arrays.asList("BTC", "BNB", "ADA", "XLM")),
                Arguments.of(true, Arrays.asList("BTC", "BNB", "ADA", "XLM")),
                Arguments.of(false, Arrays.asList("BTC", "BNB", "ADA", "LINK")),
                Arguments.of(true, Arrays.asList("BTC", "BNB", "ADA", "LINK")),
                Arguments.of(false, Arrays.asList("BTC", "BNB", "ETH", "XLM")),
                Arguments.of(true, Arrays.asList("BTC", "BNB", "ETH", "XLM")),
                Arguments.of(false, Arrays.asList("BTC", "BNB", "ETH", "LINK")),
                Arguments.of(true, Arrays.asList("BTC", "BNB", "ETH", "LINK")),
                Arguments.of(false, Arrays.asList("BTC", "BNB", "XLM", "LINK")),
                Arguments.of(true, Arrays.asList("BTC", "BNB", "XLM", "LINK")),
                Arguments.of(false, Arrays.asList("BTC", "ADA", "ETH", "XLM")),
                Arguments.of(true, Arrays.asList("BTC", "ADA", "ETH", "XLM")),
                Arguments.of(false, Arrays.asList("BTC", "ADA", "ETH", "LINK")),
                Arguments.of(true, Arrays.asList("BTC", "ADA", "ETH", "LINK")),
                Arguments.of(false, Arrays.asList("BTC", "ADA", "XLM", "LINK")),
                Arguments.of(true, Arrays.asList("BTC", "ADA", "XLM", "LINK")),
                Arguments.of(false, Arrays.asList("BTC", "ETH", "XLM", "LINK")),
                Arguments.of(true, Arrays.asList("BTC", "ETH", "XLM", "LINK")),
                Arguments.of(false, Arrays.asList("BNB", "ADA", "ETH", "XLM")),
                Arguments.of(true, Arrays.asList("BNB", "ADA", "ETH", "XLM")),
                Arguments.of(false, Arrays.asList("BNB", "ADA", "ETH", "LINK")),
                Arguments.of(true, Arrays.asList("BNB", "ADA", "ETH", "LINK")),
                Arguments.of(false, Arrays.asList("BNB", "ADA", "XLM", "LINK")),
                Arguments.of(true, Arrays.asList("BNB", "ADA", "XLM", "LINK")),
                Arguments.of(false, Arrays.asList("BNB", "ETH", "XLM", "LINK")),
                Arguments.of(true, Arrays.asList("BNB", "ETH", "XLM", "LINK")),
                Arguments.of(false, Arrays.asList("ADA", "ETH", "XLM", "LINK")),
                Arguments.of(true, Arrays.asList("ADA", "ETH", "XLM", "LINK"))
        );
    }

    @ParameterizedTest
    @MethodSource("dateProvider")
    @Disabled
    void byDayAndWeek(boolean rebalance, List<String> symbols) throws InterruptedException, ExecutionException {
//        int hours = (int) HOURS.between(LocalDateTime.parse("2021-02-10T01:56:36.099"), now()) + 24645;
        int hours = (int) HOURS.between(LocalDateTime.parse("2021-02-10T05:41:36.099"), now()) + 18095;
//        int hours = (int) HOURS.between(LocalDateTime.parse("2021-02-10T04:57:36.099"), now()) + 23587;
        LocalDateTime startingDay = now().minusHours(hours);

        Gson gson = new Gson();
//        List<String> symbols = Arrays.asList("BTC", "BNB", "ADA", "ETH");
//        List<String> symbols = Arrays.asList("BTC", "BNB", "ADA", "ETH", "XLM");

        Map<String, Coin> coins = statsService.coins(symbols, HOURLY, hours);

        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();

//        for (Integer d : Arrays.asList(1, 3, 4, 5)) {
        for (Integer d : Arrays.asList(3)) {
            for (Integer w : Arrays.asList(4)) {
//            for (Integer w : Arrays.asList(1, 2, 3, 4)) {
//                for (int h = 1; h <= 23; h += 1) {
                for (Integer h : Arrays.asList(23)) {
//                    for (int rh = 1; rh <= 23; rh += 1) {
                    for (Integer rh : Arrays.asList(23)) {
                        for (Integer r : Arrays.asList(28)) {
                            for (double t : Arrays.asList(0.1)) {
//                        for (int r = 1; r <= 91; r += 1) {
//                            for (int t = 0; t <= 25; t++) {
                                int finalDayOfWeek = d;
                                int finalWeek = w;

                                int finalHour = h;
                                int finalRebalanceHour = rh;
                                int finalRebalanceFrequency = r;
                                int finalRebalanceMinute = 59;
                                int finalMinute = 59;
//                                BigDecimal threshold = BigDecimal.valueOf(t);
                                BigDecimal threshold = BigDecimal.valueOf(0.07);
                                CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                                    try {
                                        Map<String, Coin> stringCoinMap = objectMapper.readValue(writeValueAsString, typeRef);
                                        BigDecimal calculate = statsService.calculate(startingDay, stringCoinMap, finalRebalanceFrequency, finalWeek, finalDayOfWeek,
                                                finalHour, finalRebalanceHour, threshold, rebalance, finalMinute, finalRebalanceMinute);
                                        results.put(calculate, String.format("Day %s, Hour %s, Week %s, Rebalance hour %s, Treshold %s, Freq %s, Rminute %s, Minute %s",
                                                finalDayOfWeek, finalHour, finalWeek, finalRebalanceHour, threshold, finalRebalanceFrequency, finalRebalanceMinute, finalMinute));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                });
                                futures.add(completableFuture);
                                if (!rebalance) {
                                    break;
                                }
                            }
                            if (!rebalance) {
                                break;
                            }
                        }
                        if (!rebalance) {
                            break;
                        }
                    }
                    if (!rebalance) {
                        break;
                    }
                }
                if (!rebalance) {
                    break;
                }
            }
            if (!rebalance) {
                break;
            }
        }

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        for (CompletableFuture<?> future : futures) {
            executorService.submit(() -> future);
        }

        combinedFuture.get();

        int count = 0;
        for (
                Map.Entry<BigDecimal, String> entry : results.entrySet()) {
            System.out.println(entry);
//            count++;
//            if (count == 100) {
//                break;
//            }
        }

    }

    @Test
    @Disabled
    void ad() throws InterruptedException, ExecutionException {
        int hours = (int) HOURS.between(LocalDateTime.parse("2021-02-10T01:56:36.099"), now()) + 24645;

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(Arrays.asList("BTC", "BNB", "ADA", "ETH"), HOURLY, hours);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();
        for (Integer dayOfMonth : Arrays.asList(25, 26)) {
//                for (int t = 1100 ; t < 1120; t++) {
            for (int hour = 0; hour <= 23; hour++) {
//            for (int r = 1; r <= 91; r++) {
//                        int finalDayOfWeek = dayOfWeek;
                int finalDayOfWeek = 3;
                int finalHour = hour;
                int finalRebalanceHour = 0;
//                int finalRebalanceHour = r;
                int finalRebalanceFrequency = 38;
//                int finalRebalanceFrequency = r;
                int finalDayOfMonth = dayOfMonth;
//            int finalDayOfMonth = 4;
                BigDecimal threshold = BigDecimal.valueOf(0.1115);
//                        BigDecimal threshold = BigDecimal.valueOf(0.0001 * t);
                CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
//                        BigDecimal calculate = statsService.calculateDay(coins3, finalRebalanceFrequency, finalDayOfMonth, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                        BigDecimal calculate = statsService.calculateDay(coins3, finalRebalanceFrequency, finalDayOfMonth, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                        results.put(calculate, String.format("Day %s, Hour %s, finalDayOfMonth %s, Rebalance hour %s, Treshold %s, Freq %s",
                                finalDayOfWeek, finalHour, finalDayOfMonth, finalRebalanceHour, threshold, finalRebalanceFrequency));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
                futures.add(completableFuture);
            }
//                }
//            }
        }

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        futures.stream().<Callable<?>>map(future -> () -> future).forEach(executorService::submit);
        combinedFuture.get();

        int count = 0;
        for (Map.Entry<BigDecimal, String> entry : results.entrySet()) {
            System.out.println(entry);
            count++;
            if (count == 10) {
                break;
            }
        }

    }

    @Test
    @Disabled
    void skip() throws InterruptedException, ExecutionException {
        LocalDateTime parse = LocalDateTime.parse("2018-04-20T04:45:00.00");
        LocalDateTime now = LocalDateTime.now();
        int hours = (int) ChronoUnit.HOURS.between(parse, now);

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(Arrays.asList("BTC", "BNB", "ADA", "ETH"), HOURLY, hours);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();
//        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
//        for (int dayOfMonth = 1; dayOfMonth <= 28; dayOfMonth++) {
        for (Integer dayOfMonth : Arrays.asList(25, 26)) {
//                for (int t = 1100 ; t < 1120; t++) {
            for (int i = 8; i <= 16; i++) {
//            for (int r = 1; r <= 91; r++) {
//                        int finalDayOfWeek = dayOfWeek;
                int finalDayOfWeek = 3;
                int finalHour = i;
                int finalRebalanceHour = 23;
//                int finalRebalanceHour = r;
                int finalRebalanceFrequency = 38;
//                int finalRebalanceFrequency = r;
                int finalDayOfMonth = dayOfMonth;
//            int finalDayOfMonth = 4;
                BigDecimal threshold = BigDecimal.valueOf(0.1115);
//                        BigDecimal threshold = BigDecimal.valueOf(0.0001 * t);
                CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
//                        BigDecimal calculate = statsService.calculateDay(coins3, finalRebalanceFrequency, finalDayOfMonth, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                        BigDecimal calculate = statsService.calculateDay(coins3, finalRebalanceFrequency, finalDayOfMonth, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, false);
                        results.put(calculate, String.format("Day %s, Hour %s, finalDayOfMonth %s, Rebalance hour %s, Treshold %s, Freq %s",
                                finalDayOfWeek, finalHour, finalDayOfMonth, finalRebalanceHour, threshold, finalRebalanceFrequency));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
                futures.add(completableFuture);
            }
//                }
//            }
        }

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        futures.stream().<Callable<?>>map(future -> () -> future).forEach(executorService::submit);
        combinedFuture.get();

        int count = 0;
        for (Map.Entry<BigDecimal, String> entry : results.entrySet()) {
            System.out.println(entry);
            count++;
            if (count == 10) {
                break;
            }
        }

    }

    @Test
    @Disabled
    void dd() throws InterruptedException, ExecutionException {
        LocalDateTime parse = LocalDateTime.parse("2018-04-20T04:45:00.00");
        LocalDateTime now = LocalDateTime.now();
        int hours = (int) ChronoUnit.HOURS.between(parse, now);

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(Arrays.asList("BTC", "BNB", "ADA", "ETH"), HOURLY, hours);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();
//        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
//        for (int dayOfMonth = 1; dayOfMonth <= 28; dayOfMonth++) {
        for (Integer dayOfMonth : Arrays.asList(25, 26)) {
//                for (int t = 1100 ; t < 1120; t++) {
            for (int i = 8; i <= 16; i++) {
//            for (int r = 1; r <= 91; r++) {
//                        int finalDayOfWeek = dayOfWeek;
                int finalDayOfWeek = 3;
                int finalHour = i;
                int finalRebalanceHour = 23;
//                int finalRebalanceHour = r;
                int finalRebalanceFrequency = 38;
//                int finalRebalanceFrequency = r;
                int finalDayOfMonth = dayOfMonth;
//            int finalDayOfMonth = 4;
                BigDecimal threshold = BigDecimal.valueOf(0.1115);
//                        BigDecimal threshold = BigDecimal.valueOf(0.0001 * t);
                CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
//                        BigDecimal calculate = statsService.calculateDay(coins3, finalRebalanceFrequency, finalDayOfMonth, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                        BigDecimal calculate = statsService.calculateDay(coins3, finalRebalanceFrequency, finalDayOfMonth, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                        results.put(calculate, String.format("Day %s, Hour %s, finalDayOfMonth %s, Rebalance hour %s, Treshold %s, Freq %s",
                                finalDayOfWeek, finalHour, finalDayOfMonth, finalRebalanceHour, threshold, finalRebalanceFrequency));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
                futures.add(completableFuture);
            }
//                }
//            }
        }

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        futures.stream().<Callable<?>>map(future -> () -> future).forEach(executorService::submit);
        combinedFuture.get();

        int count = 0;
        for (Map.Entry<BigDecimal, String> entry : results.entrySet()) {
            System.out.println(entry);
            count++;
            if (count == 10) {
                break;
            }
        }

    }

//    @Test
//    void name() throws JsonProcessingException {
//        Gson gson = new Gson();
//
//        Map<String, Coin> coins = statsService.coins(HOURLY, 24617);
//        String writeValueAsString = gson.toJson(coins);
//        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
//        };
//
//        Map<String, Coin> coins4 = objectMapper.readValue(writeValueAsString, typeRef);
//        int frequency = 0;
//        int dayOfMonth = 0;
//        int dayOfWeek = 3;
//        int hour = 20;
//        int rebalanceHour = 22;
//        BigDecimal threshold = BigDecimal.valueOf(0.11);
//
//        BigDecimal total = statsService.calculate(coins4, frequency, dayOfMonth, dayOfWeek, hour, rebalanceHour, threshold, false);
//        System.out.println(String.format("total %s, frequency %s, dayOfMonth %s, dayOfWeek %s, hour %s, rebalanceHour %s, threshold %s",
//                total, frequency, dayOfMonth, dayOfWeek, hour, rebalanceHour, threshold));
//    }
}