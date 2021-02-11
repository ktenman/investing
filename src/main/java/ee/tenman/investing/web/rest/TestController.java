package ee.tenman.investing.web.rest;

import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.service.RebalancingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @Resource
    BinanceService binanceService;

    @Resource
    RebalancingService rebalancingService;

    //    @GetMapping("/buy")
    public ResponseEntity<Map> buy() {
        binanceService.buyCrypto();
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rebalance")
    public ResponseEntity<Map> rebalance() {
        rebalancingService.rebalance();
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

}
