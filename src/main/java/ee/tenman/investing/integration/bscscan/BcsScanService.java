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

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 777))
    public BigDecimal fetchBalanceOf(String contractAddress, String walletAddress) {
        return bscScanApiClient.fetchTokenAccountBalance(
                walletAddress,
                contractAddress,
                secretsService.getBcsScanApiKey()
        );
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 777))
    public TokenTransferEvents fetchTokenTransferEvents(String walletAddress) {
        return bscScanApiClient.fetchTokenTransferEvents(secretsService.getWalletAddress(), walletAddress);
    }

}
