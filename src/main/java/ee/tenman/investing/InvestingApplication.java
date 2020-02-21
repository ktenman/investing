package ee.tenman.investing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvestingApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvestingApplication.class, args);
	}

}
