package ee.tenman.investing.service.stats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
    void calculatePickDays() throws JsonProcessingException {
        Gson gson = new Gson();

        Map<String, Coin> coins = statsService.coins();
        String writeValueAsString = gson.toJson(coins);
        TypeReference<HashMap<String, Coin>> typeRef = new TypeReference<HashMap<String, Coin>>() {
        };

        Map<BigDecimal, String> results = new TreeMap<>();
        List<Integer> integerList1 = new ArrayList<>();
        for (int i = 1; i < 366; i++) {
            integerList1.add(i);
        }
        List<Integer> integerList2 = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            integerList2.add(i);
        }
        Collections.shuffle(integerList1);
        Collections.shuffle(integerList2);

        for (Integer i : integerList1) {
            for (Integer j : integerList2) {
                Map<String, Coin> coins3 = objectMapper.readValue(writeValueAsString, typeRef);
                BigDecimal calculate = statsService.calculate(coins3, i, j);
                results.put(calculate, i + "---" + j);
            }
        }

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