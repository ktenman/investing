package ee.tenman.investing.service;

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
    void informAboutPortfolios() {

        informingService.informAboutPortfolios();
    }
}