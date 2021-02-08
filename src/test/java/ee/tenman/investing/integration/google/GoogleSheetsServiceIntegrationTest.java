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
    @Disabled
    void refreshCryptoPrices() throws Exception {
        long startTime = System.nanoTime();
        googleService.refreshCryptoPrices();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Disabled
    void run() {
        long startTime = System.nanoTime();
        googleService.run();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1.000_000_000;
    }
}