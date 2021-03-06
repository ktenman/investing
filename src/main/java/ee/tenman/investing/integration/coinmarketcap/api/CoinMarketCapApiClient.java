package ee.tenman.investing.integration.coinmarketcap.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "coinMarketCapApiClient", url = "https://web-api.coinmarketcap.com/v1.1/cryptocurrency/quotes")
public interface CoinMarketCapApiClient {

    String USER_AGENT = "Mozilla/5.0";

    @GetMapping(value = "/historical?format=chart_crypto_details&interval=5m", produces = "application/json")
    CoinInformation fetchCoinData(
            @RequestParam("id") Integer id,
            @RequestHeader("User-Agent") String userAgent,
            @RequestParam("time_start") long fromDateTime,
            @RequestParam("time_end") long toDateTime,
            @RequestParam("convert") String... convert
    );

    @GetMapping(value = "/historical?format=chart_crypto_details&interval=5m", produces = "application/json")
    CoinInformation fetchCoinData(
            @RequestParam("id") Integer id,
            @RequestHeader("User-Agent") String userAgent,
            @RequestParam("convert") String... convert
    );

}
