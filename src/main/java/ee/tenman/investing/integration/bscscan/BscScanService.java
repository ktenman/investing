package ee.tenman.investing.integration.bscscan;

import ee.tenman.investing.integration.bscscan.TokenTransferEvents.Event;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.service.SecretsService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static ee.tenman.investing.integration.yieldwatchnet.Symbol.SYMBOL_NAMES;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class BscScanService {

    @Resource
    private BscScanApiClient bscScanApiClient;

    @Resource
    private SecretsService secretsService;

    @Retryable(value = {FeignException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public BigDecimal getBnbBalance() {

        BigDecimal bnbBalanceResponse = bscScanApiClient.fetchBnbBalance(
                secretsService.getWalletAddress(),
                secretsService.getBcsScanApiKey()
        );

        log.info("{}", bnbBalanceResponse);

        return bnbBalanceResponse;
    }

    @Retryable(value = {FeignException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public Map<Symbol, BigDecimal> fetchSymbolBalances() {

        TokenTransferEvents tokenTransferEvents = bscScanApiClient.fetchTokenTransferEvents(
                secretsService.getWalletAddress(),
                secretsService.getBcsScanApiKey()
        );

        return tokenTransferEvents.getEvents()
                .stream()
                .filter(this::filterSymbolEvents)
                .collect(groupingBy(this::toSymbol, mapping(Event::getContractAddress, toSet())))
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> fetchBalanceOf(e.getValue().iterator().next())));
    }

    private BigDecimal fetchBalanceOf(String contractAddress) {
        return bscScanApiClient.fetchTokenAccountBalance(
                secretsService.getWalletAddress(),
                contractAddress,
                secretsService.getBcsScanApiKey()
        );
    }

    private Symbol toSymbol(Event event) {
        return Symbol.valueOf(event.getTokenSymbol().toUpperCase());
    }

    private boolean filterSymbolEvents(Event event) {
        if (StringUtils.isEmpty(event.getTokenSymbol())) {
            return false;
        }
        return SYMBOL_NAMES.contains(event.getTokenSymbol().toUpperCase());
    }

}
