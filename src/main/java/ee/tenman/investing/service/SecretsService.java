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
    private String walletAddress;

    @PostConstruct
    void setWalletAddress() {
        walletAddress = FileUtils.getSecret(walletAddressResource);
    }

    public String getWalletAddress() {
        return walletAddress;
    }
}
