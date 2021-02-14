package ee.tenman.investing.integration.coinmarketcap.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "coinMarketCapApiClient", url = "https://web-api.coinmarketcap.com/v1.1/cryptocurrency/quotes")
public interface CoinMarketCapApiClient {
    @GetMapping(value = "/historical?format=chart_crypto_details&interval=5m", produces = "application/json")
    CoinInformation fetchCoinData(
            @RequestParam("id") Integer id,
            @RequestHeader("User-Agent") String userAgent,
            @RequestParam("convert") String... convert
    );

}
