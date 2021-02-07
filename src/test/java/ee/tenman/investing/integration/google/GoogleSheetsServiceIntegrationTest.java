package ee.tenman.investing.integration.google;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class GoogleSheetsServiceIntegrationTest {

    @Resource
    GoogleSheetsService googleService;

    @Disabled
    @Test
    void updateSumOfTickers() throws Exception {
        long startTime = System.nanoTime();
        googleService.updateSumOfTickers();
        System.out.println("Time: " + ((System.nanoTime() - startTime) / 1.000_000_000) + "s");
    }

    @Disabled
    @Test
    void refreshCryptoPrices() throws Exception {
        long startTime = System.nanoTime();
        googleService.refreshCryptoPrices();
        System.out.println("Time: " + ((System.nanoTime() - startTime) / 1.000_000_000) + "s");
    }

    @Disabled
    @Test
    void run() throws Exception {
        long startTime = System.nanoTime();
        googleService.run();
        System.out.println("Time: " + ((System.nanoTime() - startTime) / 1.000_000_000) + "s");
    }
}