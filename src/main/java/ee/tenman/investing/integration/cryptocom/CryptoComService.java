package ee.tenman.investing.integration.cryptocom;

import ee.tenman.investing.exception.NotSupportedSymbolException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class CryptoComService {

    @Resource
    CryptoComApiClient cryptoComApiClient;

    @Retryable(value = {FeignException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public BigDecimal getInstrumentPrice(String from, String to) {
        String instrumentName = String.format("%s_%s", from, to);

        Instrument instrument = cryptoComApiClient.fetchInstrument(instrumentName);
        Object data = instrument.getResult().getData();

        if (data instanceof Map) {
            BigDecimal price = BigDecimal.valueOf((double) ((LinkedHashMap) data).get("b"));
            log.info("{} price {}", instrumentName, price);
            return price;
        }

        throw new NotSupportedSymbolException(String.format("%s not supported", instrumentName));
    }

}
