package ee.tenman.investing.integration.bscscan;

import ee.tenman.investing.integration.bscscan.TokenTransferEvents.Event;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BNB;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class BalanceService {

    @Resource
    private BcsScanService bcsScanService;

    @Retryable(value = {Exception.class}, maxAttempts = 20, backoff = @Backoff(delay = 100))
    public Map<String, Map<Symbol, BigDecimal>> fetchSymbolBalances(String... walletAddresses) {
        return Stream.of(walletAddresses).parallel()
                .collect(toMap(identity(), this::fetchSymbolBalance));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 20, backoff = @Backoff(delay = 100))
    public Map<Symbol, BigDecimal> fetchSymbolBalance(String walletAddress) {

        return fetchSymbolBalances(walletAddress, Arrays.asList(Symbol.values()))
                .entrySet()
                .stream()
                .filter(e -> ComparableUtils.is(e.getValue()).greaterThan(BigDecimal.ZERO))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b,
                        TreeMap::new
                ));
    }

    public Map<Symbol, BigDecimal> fetchSymbolBalances(String walletAddress, List<Symbol> symbols) {

        TokenTransferEvents tokenTransferEvents = bcsScanService.fetchTokenTransferEvents(walletAddress);

        CompletableFuture<BigDecimal> bnbBalanceFuture = CompletableFuture.supplyAsync(() -> bcsScanService.fetchBnbBalance(
                walletAddress
        ));

        Map<Symbol, BigDecimal> symbolBalances = tokenTransferEvents.getEvents()
                .parallelStream()
                .filter(this::filterSymbolEvents)
                .collect(groupingBy(this::toSymbol, mapping(Event::getContractAddress, toSet())))
                .entrySet()
                .stream()
                .filter(e -> symbols.contains(e.getKey()))
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> bcsScanService.fetchBalanceOf(e.getValue().iterator().next(), walletAddress),
                        (a, b) -> b,
                        TreeMap::new)
                );

        symbolBalances.putIfAbsent(BNB, bnbBalanceFuture.join());

        return symbolBalances;
    }

    private Symbol toSymbol(Event event) {
        return Symbol.valueOf(event.getTokenSymbol().toUpperCase());
    }

    private boolean filterSymbolEvents(Event event) {
        if (StringUtils.isEmpty(event.getTokenSymbol())) {
            return false;
        }
        return Symbol.SYMBOL_NAMES.contains(event.getTokenSymbol().toUpperCase());
    }

}
