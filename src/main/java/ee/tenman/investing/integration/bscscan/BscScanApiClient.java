package ee.tenman.investing.integration.bscscan;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "bscScanApiClient", url = "https://api.bscscan.com")
public interface BscScanApiClient {

    @GetMapping(value = "/api")
    BnbBalanceResponse fetchBnbBalance(
            @RequestParam("module") String module,
            @RequestParam("action") String action,
            @RequestParam("address") String address,
            @RequestParam("apikey") String apikey,
            @RequestHeader("User-Agent") String userAgent
    );
}