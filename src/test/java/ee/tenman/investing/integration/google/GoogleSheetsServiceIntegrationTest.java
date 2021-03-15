package ee.tenman.investing.integration.google;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@SpringBootTest
@TestPropertySource(properties = "yieldwatch.url=https://www.yieldwatch.net/api/")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GoogleSheetsServiceIntegrationTest {

    @Resource
    GoogleSheetsService googleService;

    @Test
    @Disabled
    void updateSumOfTickers() throws Exception {
        long startTime = System.nanoTime();
        googleService.updateSumOfTickers();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Order(1)
    void refreshCryptoPrices() {
        long startTime = System.nanoTime();
        googleService.refreshCryptoPrices();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Disabled
    void refreshStockPrices() throws Exception {
        long startTime = System.nanoTime();
        googleService.refreshStockPrices();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Disabled
    void appendProfits() {
        long startTime = System.nanoTime();
        googleService.appendProfits();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Order(2)
    @Disabled
    void refreshBalances() throws Exception {
        googleService.refreshBalances();
    }

    @Test
    @Order(3)
    @Disabled
    void appendYieldInformation() {
        long startTime = System.nanoTime();
        googleService.appendYieldInformation();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1.000_000_000;
    }

}