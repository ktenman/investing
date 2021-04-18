package ee.tenman.investing.integration.ecb;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversionRate {
    private LocalDate date;
    private String currency;
    private BigDecimal rate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversionRate conversionRate = (ConversionRate) o;
        return date.equals(conversionRate.date) && currency.equals(conversionRate.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, currency);
    }
}