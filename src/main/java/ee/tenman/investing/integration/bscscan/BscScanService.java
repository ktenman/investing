package ee.tenman.investing.integration.bscscan;

import ee.tenman.investing.service.SecretsService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@Slf4j
public class BscScanService {

    @Resource
    private BscScanApiClient bscScanApiClient;

    @Resource
    private SecretsService secretsService;

    @Retryable(value = {FeignException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public BigDecimal getBnbBalance() {

        BnbBalanceResponse bnbBalanceResponse = bscScanApiClient.fetchBnbBalance(
                "account",
                "balance",
                secretsService.getWalletAddress(),
                secretsService.getBcsScanApiKey(),
                "Mozilla/5.0"
        );

        log.info("{}", bnbBalanceResponse);

        return new BigDecimal(new BigInteger(bnbBalanceResponse.getResult()), 18);
    }

}
