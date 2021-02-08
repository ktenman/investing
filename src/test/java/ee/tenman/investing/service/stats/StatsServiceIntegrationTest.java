package ee.tenman.investing.service.stats;

import com.binance.api.client.domain.market.CandlestickInterval;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    void calculate() {
        Map<BigDecimal, Integer> results = new TreeMap<>();
        List<Integer> integerList = new ArrayList<>();
        for (int i = 1; i < 184; i++) {
            integerList.add(i);
        }
        Collections.shuffle(integerList);

        for (Integer i : integerList) {
            BigDecimal calculate = statsService.calculate(i, 0);
            results.put(calculate, i);
        }

        System.out.println(results);
    }

    @Test
    void calculatePickDays() throws InterruptedException, ExecutionException {
        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(HOURLY, 31211);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        List<CompletableFuture> futures = new ArrayList<>();
//        for (int day = 4; day <= 4; day++) {
//            for (int diff = 5; diff <= 5; diff++) {
        int finalDiff = 5;
        int finalDay = 4;
        int finalHour = 21;
        int finalRebalnceHour = 20;
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            try {
                Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
                BigDecimal calculate = statsService.calculate(coins3, 91, finalDiff, finalDay, finalHour, finalRebalnceHour);
                results.put(calculate, String.format("Day %s, Hour %s, Diff %s, Rebalance hour %s", finalDay, finalHour, finalDiff, finalRebalnceHour));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        futures.add(completableFuture);
//            }
//        }

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        futures.stream().<Callable<?>>map(future -> () -> future).forEach(executorService::submit);
        combinedFuture.get();

        System.out.println(results);
    }

    @Test
    void calculatePickDaysMinute() throws InterruptedException, ExecutionException {
        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins(CandlestickInterval.ONE_MINUTE, 187200);
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        List<CompletableFuture> futures = new ArrayList<>();
        for (int day = 3; day <= 4; day++) {
            for (int minute = 0; minute <= 59; minute++) {
                for (int diff = 4; diff <= 5; diff++) {
                    int finalDiff = diff;
                    int finalMinute = minute;
                    int finalDay = day;
                    int finalHour = 21;
                    int finalRebalnceHour = 20;
                    CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                        try {
                            Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
                            BigDecimal calculate = statsService.calculateMinute(coins3, 91, finalDiff, finalDay, finalHour, finalRebalnceHour, finalMinute);
                            results.put(calculate, String.format("Day %s, Hour %s, Diff %s, Rebalance hour %s, Minute %s", finalDay, finalHour, finalDiff, finalRebalnceHour, finalMinute));
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    });
                    futures.add(completableFuture);
                }
            }
        }

        futures.stream().<Callable<?>>map(future -> () -> future).forEach(executorService::submit);
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        combinedFuture.get();

        System.out.println(results);
    }

    @Test
    void calculate103() {
        System.out.println(statsService.calculate(96, 0));
        System.out.println(statsService.calculate(103, 0));
    }

    @Test
    void calculate1() {
        System.out.println(statsService.calculate(1, 0));
    }
}