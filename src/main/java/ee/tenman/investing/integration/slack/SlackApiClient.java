package ee.tenman.investing.integration.slack;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "slackApiClient", url = "${slack.url:https://hooks.slack.com/}")
public interface SlackApiClient {

    @PostMapping(value = "services/{id}/{key}/{secret}", produces = "application/json")
    void post(
            @PathVariable("id") String id,
            @PathVariable("key") String key,
            @PathVariable("secret") String secret,
            @RequestHeader("User-Agent") String userAgent,
            @RequestBody SlackMessage slackMessage
    );
}
