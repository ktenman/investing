package ee.tenman.investing.integration.slack;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SlackMessage {
    private String text;
}
