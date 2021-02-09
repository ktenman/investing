package ee.tenman.investing.service.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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

import static com.binance.api.client.domain.market.CandlestickInterval.HOURLY;

@SpringBootTest
class StatsServiceIntegrationTest {

    @Resource
    private StatsService statsService;

    @Resource
    private ObjectMapper objectMapper;

    @Test
    @Disabled
    void bw() throws InterruptedException, ExecutionException {
        LocalDateTime parse = LocalDateTime.parse("2018-04-20T04:45:00.00");
        LocalDateTime now = LocalDateTime.now();
        int hours = (int) ChronoUnit.HOURS.between(parse, now);

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(HOURLY, hours);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i <= 23; i++) {
            int finalDayOfWeek = 3;
            int finalHour = i;
            int finalRebalanceHour = 23;
            int finalRebalanceFrequency = 38;
            int finalWeek = 4;
            BigDecimal threshold = BigDecimal.valueOf(0.1115);
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
                    BigDecimal calculate = statsService.calculate(coins3, finalRebalanceFrequency, finalWeek, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                    results.put(calculate, String.format("Day %s, Hour %s, Week %s, Rebalance hour %s, Treshold %s, Freq %s",
                            finalDayOfWeek, finalHour, finalWeek, finalRebalanceHour, threshold, finalRebalanceFrequency));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
            futures.add(completableFuture);
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
    void bWeekByMinute() throws InterruptedException, ExecutionException {
        LocalDateTime parse = LocalDateTime.parse("2018-04-20T04:45:00.00");
        LocalDateTime now = LocalDateTime.now();
        int hours = (int) ChronoUnit.HOURS.between(parse, now);

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(HOURLY, hours);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i <= 23; i++) {
            int finalDayOfWeek = 3;
            int finalHour = i;
            int finalRebalanceHour = 23;
            int finalRebalanceFrequency = 38;
            int finalWeek = 4;
            BigDecimal threshold = BigDecimal.valueOf(0.1115);
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
                    BigDecimal calculate = statsService.calculate(coins3, finalRebalanceFrequency, finalWeek, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                    results.put(calculate, String.format("Day %s, Hour %s, Week %s, Rebalance hour %s, Treshold %s, Freq %s",
                            finalDayOfWeek, finalHour, finalWeek, finalRebalanceHour, threshold, finalRebalanceFrequency));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
            futures.add(completableFuture);
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
    void ad() throws InterruptedException, ExecutionException {
        LocalDateTime parse = LocalDateTime.parse("2018-04-20T04:45:00.00");
        LocalDateTime now = LocalDateTime.now();
        int hours = (int) ChronoUnit.HOURS.between(parse, now);

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(HOURLY, hours);
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

    @Test
    @Disabled
    void cw() throws InterruptedException, ExecutionException {
        LocalDateTime parse = LocalDateTime.parse("2018-04-20T04:45:00.00");
        LocalDateTime now = LocalDateTime.now();
        int hours = (int) ChronoUnit.HOURS.between(parse, now);

        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(HOURLY, hours);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>(Collections.reverseOrder());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<CompletableFuture> futures = new ArrayList<>();
//        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
//            for (int week = 1; week <= 4; week++) {
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
//                        int finalWeek = week;
            int finalWeek = 4;
            BigDecimal threshold = BigDecimal.valueOf(0.1115);
//                        BigDecimal threshold = BigDecimal.valueOf(0.0001 * t);
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
                    BigDecimal calculate = statsService.calculate(coins3, finalRebalanceFrequency, finalWeek, finalDayOfWeek, finalHour, finalRebalanceHour, threshold, true);
                    results.put(calculate, String.format("Day %s, Hour %s, Week %s, Rebalance hour %s, Treshold %s, Freq %s",
                            finalDayOfWeek, finalHour, finalWeek, finalRebalanceHour, threshold, finalRebalanceFrequency));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
            futures.add(completableFuture);
//            }
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

        Map<String, Coin> coins = statsService.coins(HOURLY, hours);
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