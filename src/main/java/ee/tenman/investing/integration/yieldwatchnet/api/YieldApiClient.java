package ee.tenman.investing.integration.yieldwatchnet.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import static ee.tenman.investing.integration.slack.SlackService.USER_AGENT;

@FeignClient(value = "yieldApiClient", url = "${yieldwatch.url:https://www.yieldwatch.net/api/}")
public interface YieldApiClient {

    @GetMapping(value = "all/{walletAddress}", produces = MediaType.APPLICATION_JSON_VALUE)
    YieldData fetchYieldData(
            @PathVariable("walletAddress") String walletAddress,
            @RequestHeader("User-Agent") String userAgent,
            @RequestParam("platforms") String... platforms
    );

    default YieldData fetchYieldData(@PathVariable("walletAddress") String walletAddress) {
        return fetchYieldData(walletAddress, USER_AGENT, "beefy", "auto", "pancake");
    }
}