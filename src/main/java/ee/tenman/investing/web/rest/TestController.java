package ee.tenman.investing.web.rest;

import ee.tenman.investing.domain.Portfolio;
import ee.tenman.investing.integration.bscscan.BscScanService;
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

    @Resource
    private BscScanService bscScanService;

    @GetMapping("/portfolios")
    public ResponseEntity<List<Portfolio>> portfolios() {
        List<Portfolio> portfolioTotalValues = informingService.getPortfolioTotalValues();

//        portfolioTotalValues.stream().parallel()
//                .forEach(portfolioTotalValue -> portfolioTotalValue.setTokenBalances(
//                        bscScanService.fetchSymbolBalances(portfolioTotalValue.getWalletAddress())
//                ));

        return ResponseEntity.ok(portfolioTotalValues);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<Symbol, String>> performance() {
        Map<Symbol, String> differencesIn24Hours = informingService.getDifferencesIn24Hours();
        return ResponseEntity.ok(differencesIn24Hours);
    }

}
