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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BNB;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.SYMBOL_NAMES;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class BalanceService {

    @Resource
    private BscScanApiClient bscScanApiClient;

    @Resource
    private SecretsService secretsService;

    @Resource
    private BcsScanService bcsScanService;

    @Retryable(value = {FeignException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public BigDecimal getBnbBalance() {

        BigDecimal bnbBalanceResponse = bscScanApiClient.fetchBnbBalance(
                secretsService.getWalletAddress(),
                secretsService.getBcsScanApiKey()
        );

        log.info("{}", bnbBalanceResponse);

        return bnbBalanceResponse;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(delay = 333))
    public Map<String, Map<Symbol, BigDecimal>> fetchSymbolBalances(List<String> walletAddresses) {
        return walletAddresses.stream()
                .collect(toMap(identity(), this::fetchSymbolBalances));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<Symbol, BigDecimal> fetchSymbolBalances(String walletAddress) {

        return fetchSymbolBalances(walletAddress, new HashSet<>(Arrays.asList(Symbol.values())));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<Symbol, BigDecimal> fetchSymbolBalances(String walletAddress, Set<Symbol> symbolSet) {

        TokenTransferEvents tokenTransferEvents = bcsScanService.fetchTokenTransferEvents(walletAddress);

        Map<Symbol, BigDecimal> symbolBalances = tokenTransferEvents.getEvents()
                .parallelStream()
                .filter(this::filterSymbolEvents)
                .collect(groupingBy(this::toSymbol, mapping(Event::getContractAddress, toSet())))
                .entrySet()
                .stream()
                .filter(e -> symbolSet.contains(e.getKey()))
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> bcsScanService.fetchBalanceOf(e.getValue().iterator().next(), walletAddress),
                        (a, b) -> b,
                        TreeMap::new)
                );

        symbolBalances.putIfAbsent(BNB, bscScanApiClient.fetchBnbBalance(
                walletAddress,
                secretsService.getBcsScanApiKey()
        ));

        return symbolBalances;
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
