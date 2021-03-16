package ee.tenman.investing.web.rest;

import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.domain.Portfolio;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.service.InformingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    @Resource
    private InformingService informingService;

    @GetMapping("/portfolios")
    public ResponseEntity<ImmutableMap<String, Object>> portfolios() {
        long start = System.nanoTime();
        List<Portfolio> portfolios = informingService.getPortfolioTotalValues();
        ImmutableMap<String, Object> response = ImmutableMap.of(
                "duration_in_seconds", duration(start),
                "portfolios", portfolios
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<Symbol, String>> performance() {
        Map<Symbol, String> differencesIn24Hours = informingService.getDifferencesIn24Hours();
        return ResponseEntity.ok(differencesIn24Hours);
    }

    private double duration(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000_000.0;
    }

}
