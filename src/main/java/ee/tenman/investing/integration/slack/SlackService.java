package ee.tenman.investing.integration.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class SlackService {

    public static final String USER_AGENT = "Mozilla/5.0";

    @Value("${slack.id}")
    private String id;

    @Value("${slack.key}")
    private String key;

    @Value("${slack.secret}")
    private String secret;

    @Value("${slack.enabled}")
    private boolean slackEnabled;

    @Resource
    private SlackApiClient slackApiClient;

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void post(SlackMessage slackMessage) {
        if (!slackEnabled) {
            log.info("Slack is disabled. Not sending text message");
            return;
        }
        slackApiClient.post(id, key, secret, USER_AGENT, slackMessage);
    }

}
