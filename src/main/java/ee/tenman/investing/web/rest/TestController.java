package ee.tenman.investing.web.rest;

import ee.tenman.investing.domain.PerformanceResponse;
import ee.tenman.investing.domain.PortfoliosResponse;
import ee.tenman.investing.service.InformingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TestController {

    @Resource
    private InformingService informingService;

    @GetMapping("/portfolios")
    public ResponseEntity<PortfoliosResponse> portfolios(@RequestParam(name = "addresses", required = false) String... walletAddresses) {
        long startTime = System.nanoTime();
        PortfoliosResponse portfoliosResponse = informingService.getPortfolioTotalValues(walletAddresses);
        portfoliosResponse.setResponseTimeInSeconds(duration(startTime));
        return ResponseEntity.ok(portfoliosResponse);
    }

    @GetMapping("/performance")
    public ResponseEntity<PerformanceResponse> performance() {
        long startTime = System.nanoTime();
        PerformanceResponse performanceResponse = PerformanceResponse.builder()
                .differencesIn24Hours(informingService.getDifferencesIn24Hours())
                .build();
        performanceResponse.setResponseTimeInSeconds(duration(startTime));
        return ResponseEntity.ok(performanceResponse);
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000_000.0;
    }

}
