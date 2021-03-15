package ee.tenman.investing.integration.yieldwatchnet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolTest {

    @Test
    @DisplayName("Supported currency symbol should return id")
    void getCoinMarketCapId() {
        int id = Symbol.BDO.coinMarketCapId();

        assertThat(id).isEqualTo(8219);
    }

    @ParameterizedTest
    @DisplayName("Check if all symbols do have relevant id")
    @EnumSource(Symbol.class)
    void getCoinMarketCapId2(Symbol symbol) {
        int id = symbol.coinMarketCapId();

        assertThat(id).isPositive().isNotZero();
    }

}