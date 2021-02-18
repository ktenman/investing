package ee.tenman.investing.integration.yieldwatchnet.api;

import ee.tenman.investing.service.SecretsService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class YieldApiService {

    @Resource
    private YieldApiClient yieldApiClient;

    @Resource
    private SecretsService secretsService;

    @Retryable(value = {FeignException.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public YieldData getYieldData() {
        return yieldApiClient.fetchYieldData(secretsService.getWalletAddress(), "beefy", "auto", "venus");
    }

}
