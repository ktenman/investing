package ee.tenman.investing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Stats {
    private String priceChange;
    private BigDecimal currentPrice;
    @JsonProperty("price_24_hours_ago")
    private BigDecimal price24HoursAgo;

    @Override
    public String toString() {
        return priceChange;
    }
}
