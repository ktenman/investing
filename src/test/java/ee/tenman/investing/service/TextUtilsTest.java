package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @Test
    void removeCommas() {
        String comma = TextUtils.removeCommas("1.176,00");

        assertThat(comma).isEqualTo("1176.00");
    }

    @Test
    void removeCommas2() {
        String comma = TextUtils.removeCommas("1,176.00");

        assertThat(comma).isEqualTo("1176.00");
    }
}