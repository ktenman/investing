package ee.tenman.investing.service;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import ee.tenman.investing.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@Slf4j
public class BinanceApiClientConfiguration {

    @Value("binance_api_key.txt")
    ClassPathResource binanceApiKey;
    @Value("binance_secret_key.txt")
    ClassPathResource binanceSecretKey;

    @Bean
    public BinanceApiRestClient binanceApiRestClient() {
        String key = FileUtils.getSecret(binanceApiKey);
        String secret = FileUtils.getSecret(binanceSecretKey);

        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(key, secret);
        return factory.newRestClient();
    }

}
