package ee.tenman.investing.integration.google;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
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
    void refreshCryptoPrices() throws Exception {
        long startTime = System.nanoTime();
        googleService.refreshCryptoPrices();
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
    void refreshBalances() throws Exception {
        googleService.refreshBalances();
    }

    @Test
    void appendYieldInformation() {
//        googleService.appendYieldInformation();
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1.000_000_000;
    }
}