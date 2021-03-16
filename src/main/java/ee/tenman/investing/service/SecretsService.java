package ee.tenman.investing.service;

import ee.tenman.investing.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@Service
public class SecretsService {
    @Value("wallet_address.txt")
    ClassPathResource walletAddressResource;
    @Value("bcsscan_api_key.txt")
    ClassPathResource bcsScanApiKeyResource;
    private String walletAddress;
    private List<String> bcsScanApiKeys = new ArrayList<>();

    @PostConstruct
    void setWalletAddress() {
        walletAddress = FileUtils.getSecret(walletAddressResource);
    }

    @PostConstruct
    void setBcsScanApiKey() {
        String keys = FileUtils.getSecret(bcsScanApiKeyResource);
        this.bcsScanApiKeys = Pattern.compile(",")
                .splitAsStream(keys)
                .collect(toList());
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getBcsScanApiKey() {
        return bcsScanApiKeys.get(RandomUtils.nextInt(0, bcsScanApiKeys.size()));
    }

    public List<String> getBcsScanApiKeys() {
        return bcsScanApiKeys;
    }
}
