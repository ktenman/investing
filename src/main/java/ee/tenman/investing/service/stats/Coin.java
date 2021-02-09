package ee.tenman.investing.service.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Coin {
    private String name;
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO.setScale(8, BigDecimal.ROUND_UP);
    @Builder.Default
    private Map<LocalDateTime, BigDecimal> prices = new HashMap<>();

    public BigDecimal getPrice(LocalDateTime localDateTime) {
        LocalDateTime base = prices.keySet().iterator().next();
        LocalDateTime key =
                LocalDateTime.of(
                        localDateTime.getYear(),
                        localDateTime.getMonth(),
                        localDateTime.getDayOfMonth(),
                        localDateTime.getHour(),
                        localDateTime.getMinute(),
                        base.getSecond(),
                        base.getNano());
        BigDecimal bigDecimal = this.prices.get(key);
        while (bigDecimal == null) {
            key = key.plusMinutes(1);
            bigDecimal = this.prices.get(key);
        }
        return bigDecimal;
    }

    public BigDecimal getPrice(LocalDate localDate) {
        LocalDateTime localDateTime = prices.keySet().iterator().next();
        LocalDateTime key = LocalDateTime.of(localDate, localDateTime.toLocalTime());
        return this.prices.get(key);
    }

    public BigDecimal valueInUsdt(LocalDate localDate) {
        BigDecimal price = getPrice(localDate);
        return price.multiply(amount);
    }

    public BigDecimal valueInUsdt(LocalDateTime localDateTime) {
        BigDecimal price = getPrice(localDateTime);
        return price.multiply(amount);
    }
}
