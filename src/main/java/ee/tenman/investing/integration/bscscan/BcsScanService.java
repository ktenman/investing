package ee.tenman.investing.integration.bscscan;

import ee.tenman.investing.service.SecretsService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class BcsScanService {

    @Resource
    private BscScanApiClient bscScanApiClient;

    @Resource
    private SecretsService secretsService;

    @Retryable(value = {Exception.class}, maxAttempts = 15, backoff = @Backoff(delay = 20))
    public BigDecimal fetchBalanceOf(String contractAddress, String walletAddress) {
        return bscScanApiClient.fetchTokenAccountBalance(
                walletAddress,
                contractAddress,
                secretsService.getBcsScanApiKey()
        );
    }

    @Retryable(value = {Exception.class}, maxAttempts = 15, backoff = @Backoff(delay = 20))
    public BigDecimal fetchBnbBalance(String walletAddress) {
        return bscScanApiClient.fetchBnbBalance(
                walletAddress,
                secretsService.getBcsScanApiKey()
        );
    }

    @Retryable(value = {Exception.class}, maxAttempts = 15, backoff = @Backoff(delay = 20))
    public TokenTransferEvents fetchTokenTransferEvents(String walletAddress) {
        return bscScanApiClient.fetchTokenTransferEvents(walletAddress, secretsService.getBcsScanApiKey());
    }

}
