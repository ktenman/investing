package ee.tenman.investing.integration.ecb;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
class ECBClient {

    private static final String DAILY_RATES_URL = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private static final String HISTORY_90_DAYS_URL = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml";
    private final RestTemplate restTemplate;
    private List<ConversionRate> rates;

    @Resource
    private ECBUnmarshaller unmarshaller;

    public ECBClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public List<ConversionRate> getHistoryRatesForLast90days() {
        return getRatesFromUrl(HISTORY_90_DAYS_URL);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void setRates() {
        this.rates = getDailyRates();
    }

    public List<ConversionRate> getRates() {
        if (rates == null || rates.isEmpty()) {
            setRates();
        }
        return rates;
    }

    public List<ConversionRate> getDailyRates() {
        return getRatesFromUrl(DAILY_RATES_URL);
    }

    private List<ConversionRate> getRatesFromUrl(String url) {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        try {
            return unmarshaller.apply(responseEntity.getBody());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}