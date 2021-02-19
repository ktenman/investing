package ee.tenman.investing.service;

import ee.tenman.investing.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class SecretsService {
    @Value("wallet_address.txt")
    ClassPathResource walletAddressResource;
    @Value("wallet_address_ik.txt")
    ClassPathResource walletAddressResourceIK;
    @Value("bcsscan_api_key.txt")
    ClassPathResource bcsScanApiKeyResource;
    private String walletAddress;
    private String walletAddressIK;
    private String bcsScanApiKey;

    @PostConstruct
    void setWalletAddress() {
        walletAddress = FileUtils.getSecret(walletAddressResource);
    }

    @PostConstruct
    void setWalletAddressIK() {
        walletAddressIK = FileUtils.getSecret(walletAddressResourceIK);
    }

    @PostConstruct
    void setBcsScanApiKey() {
        this.bcsScanApiKey = FileUtils.getSecret(bcsScanApiKeyResource);
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getWalletAddressIK() {
        return walletAddressIK;
    }

    public String getBcsScanApiKey() {
        return bcsScanApiKey;
    }
}
