package ee.tenman.investing.web.rest;

import ee.tenman.investing.domain.PortfoliosResponse;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.service.InformingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class TestController {

    @Resource
    private InformingService informingService;

    @GetMapping("/portfolios")
    public ResponseEntity<PortfoliosResponse> portfolios(@RequestParam(name = "addresses", required = false) String... walletAddresses) {
        if (walletAddresses == null || walletAddresses.length == 0) {
            PortfoliosResponse portfoliosResponse = informingService.getPortfolioTotalValues();
            return ResponseEntity.ok(portfoliosResponse);
        }
        PortfoliosResponse portfoliosResponse = informingService.getPortfolioTotalValues(walletAddresses);
        return ResponseEntity.ok(portfoliosResponse);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<Symbol, String>> performance() {
        Map<Symbol, String> differencesIn24Hours = informingService.getDifferencesIn24Hours();
        return ResponseEntity.ok(differencesIn24Hours);
    }

}
