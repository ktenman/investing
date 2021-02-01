package ee.tenman.investing.service;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.util.stream.Collectors.joining;

@Service
@Slf4j
public class BinanceService {

    @Value("binance_api_key.txt")
    ClassPathResource binanceApiKey;
    @Value("binance_secret_key.txt")
    ClassPathResource binanceSecretKey;

    @Scheduled(cron = "0 0 12 1 * ?")
    public void buyCrypto() {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(getSecret(binanceApiKey), getSecret(binanceSecretKey));
        BinanceApiRestClient client = factory.newRestClient();
        client.ping();
        log.info("");
    }

    private String getSecret(ClassPathResource classPathResource) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()))) {
            return buffer.lines().collect(joining(""));
        } catch (IOException e) {
            log.error("getPrivateKeyId ", e);
            return null;
        }
    }

}
