package ee.tenman.investing.integration.google;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GoogleSheetsServiceTest {

    @InjectMocks
    GoogleSheetsService googleSheetsService;

    @Test
    void continueWorking() {

        assertThat(IntStream.range(0, 999)
                .parallel()
                .anyMatch(i -> googleSheetsService.continueWorking())
        ).isTrue();
    }
}