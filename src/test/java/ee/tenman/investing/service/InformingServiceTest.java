package ee.tenman.investing.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@SpringBootTest
@TestPropertySource(properties = "yieldwatch.url=https://yieldwatch.net/api/")
class InformingServiceTest {

    @Resource
    private InformingService informingService;

    @Test
    @Disabled
    void informAboutPortfolios() {
        long startTime = System.nanoTime();
        informingService.informAboutPortfolios();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    @Test
    @Disabled
    void informAboutPerformance() {
        long startTime = System.nanoTime();
        informingService.informAboutPerformance();
        System.out.println(String.format("Duration %ss", duration(startTime)));
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1.000_000_000;
    }
}