package ee.tenman.investing.integration.yieldwatchnet.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "yieldApiClient", url = "https://yieldwatch.net/api/")
public interface YieldApiClient {

    @GetMapping(value = "/all/{walletAddress}", produces = "application/json")
    YieldData fetchYieldData(
            @RequestParam("walletAddress") String walletAddress,
            @RequestParam("platforms") String... platforms
    );
}