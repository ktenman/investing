package ee.tenman.investing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PerformanceResponse extends GenericResponse {
    @JsonProperty("differences_in_24_hours")
    private Map<Symbol, Stats> differencesIn24Hours;
}
