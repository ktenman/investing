package ee.tenman.investing.cryptocom;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "cryptoComApiClient", url = "https://api.crypto.com/v2/public")
public interface CryptoComApiClient {

    @GetMapping(value = "/get-ticker", produces = "application/json")
    Instrument fetchInstrument(@RequestParam("instrument_name") String instrumentName);
}