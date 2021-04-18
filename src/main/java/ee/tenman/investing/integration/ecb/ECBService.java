package ee.tenman.investing.integration.ecb;

import ee.tenman.investing.domain.Currency;
import ee.tenman.investing.exception.NotSupportedSymbolException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Optional;

import static ee.tenman.investing.domain.Currency.EUR;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Service
public class ECBService {

    @Resource
    private ECBClient ecbClient;

    private Optional<BigDecimal> findConversionRate(Currency currency) {
        if (currency == EUR) {
            return Optional.of(BigDecimal.ONE);
        }
        return ecbClient.getRates()
                .stream()
                .filter(r -> equalsIgnoreCase(r.getCurrency(), currency.name()))
                .findFirst()
                .map(ConversionRate::getRate);
    }

    public BigDecimal convert(Currency fromCurrency, Currency toCurrency) {
        if (!isSupported(fromCurrency)) {
            throw new NotSupportedSymbolException(String.format("%s from currency failed", fromCurrency));
        }
        if (!isSupported(toCurrency)) {
            throw new NotSupportedSymbolException(String.format("%s to currency failed", toCurrency));
        }
        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE;
        }
        BigDecimal from = findConversionRate(fromCurrency).orElseThrow(
                () -> new NotSupportedSymbolException(String.format("%s from currency failed", fromCurrency))
        );
        BigDecimal to = findConversionRate(toCurrency).orElseThrow(
                () -> new NotSupportedSymbolException(String.format("%s to currency failed", toCurrency))
        );
        if (EUR == fromCurrency || EUR == toCurrency) {
            return to.divide(from, 10, BigDecimal.ROUND_UP);
        }
        from = convert(EUR, fromCurrency);
        to = convert(EUR, toCurrency);
        return to.divide(from, 10, BigDecimal.ROUND_UP);
    }

    public boolean isSupported(Currency currency) {
        if (equalsIgnoreCase(EUR.name(), currency.name())) {
            return true;
        }
        return ecbClient.getRates().stream()
                .anyMatch(r -> StringUtils.equalsIgnoreCase(r.getCurrency(), currency.name()));
    }

}
