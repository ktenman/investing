package ee.tenman.investing;

import com.codeborne.selenide.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableFeignClients
@EnableAsync
public class InvestingApplication {

	static {
		Configuration.startMaximized = true;
		Configuration.headless = true;
		Configuration.proxyEnabled = false;
		Configuration.screenshots = false;
		Configuration.browser = "firefox";
	}

	public static void main(String[] args) {
		SpringApplication.run(InvestingApplication.class, args);
	}

}
