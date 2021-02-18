package ee.tenman.investing.integration.google;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

@SpringBootTest
@TestPropertySource(properties = "yieldwatch.url=https://yieldwatch.net/api/")
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
    @Disabled
    void appendYieldInformation() throws ExecutionException, InterruptedException {
        long startTime = System.nanoTime();
        googleService.appendYieldInformation();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Disabled
    void appendYieldInformationIK() {
        long startTime = System.nanoTime();
        googleService.appendYieldInformationIK();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1.000_000_000;
    }

}